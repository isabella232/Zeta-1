CREATE MULTISET VOLATILE TABLE V_hre_volume AS (
SELECT numeric_id AS user_id
,COUNT(case_id_user) AS referral_count
,MIN(date_reported) AS first_case
,MAX(date_reported) AS last_case
    FROM ebay_ts_v.cw_reported_user usr
    JOIN ebay_ts_v.cw_skillset sk ON usr.policy_id = sk.policy_id AND usr.site_id = sk.site_id
WHERE date_reported BETWEEN DATE - 5 AND DATE - 1
 AND NAME IN ( 'HIGH Risk Email'  ) 
   GROUP BY 1
) WITH DATA PRIMARY INDEX (user_id)
    ON COMMIT PRESERVE ROWS;
    
CREATE MULTISET VOLATILE TABLE v_att_trans_m2m AS (
SELECT created_dt, 
       buyer_id, 
       seller_id,
       item_id, 
       transaction_id,
       checkout_status,
       SALE_TYPE,
        AUCT_END_DT,
        trans_site_id,
        paid_dt
FROM access_views.dw_checkout_trans t   
    JOIN V_hre_volume e ON t.buyer_id = e.user_id   
UNION ALL 
SELECT created_dt, 
       buyer_id, 
       seller_id,
       item_id, 
       transaction_id,
       checkout_status,
       SALE_TYPE,
        AUCT_END_DT,
        trans_site_id,
        paid_dt
FROM access_views.dw_checkout_trans t   
    JOIN V_hre_volume e ON t.seller_id = e.user_id  
) WITH DATA PRIMARY INDEX (item_id, transaction_id)
    ON COMMIT PRESERVE ROWS;
CREATE  MULTISET VOLATILE TABLE v_att_trans_m2m1 , NO FALLBACK ,
NO BEFORE JOURNAL,
NO AFTER JOURNAL,
CHECKSUM = DEFAULT,
DEFAULT MERGEBLOCKRATIO AS(
SELECT t.*,
      m.email_tracking_id,
      m.PARENT_EMAIL_TRACKING_ID,
      m.sndr_id,
      m.rcpnt_id,
      m.email_type_id,
      m.msg_type_id,
      m.src_cre_dt,
      m.src_cre_date, 
      RANK() OVER( PARTITION BY t.item_id,t.buyer_id, t.seller_id ORDER BY m.src_cre_date ASC) AS email_seq
FROM v_att_trans_m2m t
LEFT JOIN access_views.dw_ue_email_tracking m
ON t.item_id = m.item_id 
AND
( ( m.sndr_id = t.buyer_id AND m.rcpnt_id = t.seller_id )
OR ( m.rcpnt_id = t.buyer_id AND m.sndr_id = t.seller_id ))
    WHERE m.src_cre_dt >= 7
) WITH DATA PRIMARY INDEX (item_id, transaction_id)
    ON COMMIT PRESERVE ROWS;
COLLECT STATS ON v_att_trans_m2m1 INDEX(item_id, transaction_id);
COLLECT STATS ON v_att_trans_m2m1 COLUMN(item_id);
COLLECT STATS ON v_att_trans_m2m1 COLUMN(transaction_id);
COLLECT STATS ON v_att_trans_m2m1 COLUMN(created_dt);
COLLECT STATS ON v_att_trans_m2m1 COLUMN(checkout_status);
COLLECT STATS ON v_att_trans_m2m1 COLUMN(trans_site_id);
COLLECT STATS ON v_att_trans_m2m1 COLUMN(email_tracking_id);
COLLECT STATS ON v_att_trans_m2m1 COLUMN(PARENT_EMAIL_TRACKING_ID);
COLLECT STATS ON v_att_trans_m2m1 COLUMN(sndr_id);
COLLECT STATS ON v_att_trans_m2m1 COLUMN(rcpnt_id);
COLLECT STATS ON v_att_trans_m2m1 COLUMN(email_type_id);
COLLECT STATS ON v_att_trans_m2m1 COLUMN(msg_type_id);
COLLECT STATS ON v_att_trans_m2m1 COLUMN(src_cre_dt);
COLLECT STATS ON v_att_trans_m2m1  COLUMN(src_cre_date);
CREATE  MULTISET VOLATILE TABLE v_att_trans_m2m2 , NO FALLBACK ,
NO BEFORE JOURNAL,
NO AFTER JOURNAL,
CHECKSUM = DEFAULT,
DEFAULT MERGEBLOCKRATIO AS(
SELECT created_dt,
       item_id,
       buyer_id,
       seller_id,
       transaction_id,
       sale_type,
       trans_site_id,
       checkout_status,
       SUM(CASE WHEN src_cre_dt <= paid_dt THEN 1 ELSE 0 end) AS Pre_Payment,
       SUM(CASE WHEN src_cre_dt > paid_dt THEN 1 ELSE 0 end) AS Post_Payment,
       SUM(CASE WHEN email_seq = 1 AND buyer_id = sndr_id THEN 1 ELSE 0 end) AS Buyer_Init,
       SUM(CASE WHEN email_seq = 1 AND seller_id = sndr_id THEN 1 ELSE 0 end) AS Seller_Init,
       SUM(CASE WHEN buyer_id = sndr_id THEN 1 ELSE 0 end) AS Buyer_Msgs,
        SUM(CASE WHEN seller_id = sndr_id THEN 1 ELSE 0 end) AS Seller_Msgs
FROM v_att_trans_m2m1
GROUP BY 1,2,3,4,5,6,7,8
) WITH DATA PRIMARY INDEX (item_id, transaction_id) ON COMMIT PRESERVE ROWS;
CREATE  MULTISET VOLATILE TABLE v_att_trans_m2m_buy , NO FALLBACK ,
NO BEFORE JOURNAL,
NO AFTER JOURNAL,
CHECKSUM = DEFAULT,
DEFAULT MERGEBLOCKRATIO AS(
SELECT user_id
,CASE WHEN 1=1 THEN 'b' ELSE NULL END AS m2m_ROLE
      ,SUM( CASE WHEN buyer_msgs > 0 OR seller_msgs > 0 THEN 1 ELSE 0 end) AS Trans_with_M2M
       ,SUM( CASE WHEN Pre_payment > 0 THEN 1 ELSE 0 end) AS Trans_with_M2M_Pre_Pay
       ,SUM( CASE WHEN Post_payment > 0 THEN 1 ELSE 0 end) AS Trans_with_M2M_Post_Pay
     , COUNT(*) AS Total_Trans
      ,SUM( buyer_msgs) AS M2M_Trans_Buyer
       ,SUM( seller_msgs) AS M2M_Trans_Seller
       ,SUM( Pre_payment) AS M2M_Pre
       ,SUM( Post_payment) AS M2M_Post
       ,SUM( buyer_init) AS M2M_Buyer_Init
       ,SUM( seller_init) AS M2M_Seller_Init
FROM v_att_trans_m2m2 m
    JOIN V_hre_volume e ON e.user_Id = m.buyer_id
GROUP BY 1
) WITH DATA PRIMARY INDEX (user_id) ON COMMIT PRESERVE ROWS;
CREATE  MULTISET VOLATILE TABLE v_att_trans_m2m_sell , NO FALLBACK ,
NO BEFORE JOURNAL,
NO AFTER JOURNAL,
CHECKSUM = DEFAULT,
DEFAULT MERGEBLOCKRATIO AS(
        
  SELECT user_id
,CASE WHEN 1=1 THEN 's' ELSE NULL END AS m2m_ROLE
      ,SUM( CASE WHEN buyer_msgs > 0 OR seller_msgs > 0 THEN 1 ELSE 0 end) AS Trans_with_M2M
       ,SUM( CASE WHEN Pre_payment > 0 THEN 1 ELSE 0 end) AS Trans_with_M2M_Pre_Pay
       ,SUM( CASE WHEN Post_payment > 0 THEN 1 ELSE 0 end) AS Trans_with_M2M_Post_Pay
     , COUNT(*) AS Total_Trans
      ,SUM( buyer_msgs) AS M2M_Trans_Buyer
       ,SUM( seller_msgs) AS M2M_Trans_Seller
       ,SUM( Pre_payment) AS M2M_Pre
       ,SUM( Post_payment) AS M2M_Post
       ,SUM( buyer_init) AS M2M_Buyer_Init
       ,SUM( seller_init) AS M2M_Seller_Init
FROM v_att_trans_m2m2 m
    JOIN V_hre_volume e ON e.user_Id = m.seller_id
GROUP BY 1
) WITH DATA PRIMARY INDEX (user_id) ON COMMIT PRESERVE ROWS;
    
    
CREATE  MULTISET VOLATILE TABLE v_no_trans_m2m_buy , NO FALLBACK ,
NO BEFORE JOURNAL,
NO AFTER JOURNAL,
CHECKSUM = DEFAULT,
DEFAULT MERGEBLOCKRATIO AS(
        
    SELECT sndr_id 
    ,CASE WHEN 1=1 THEN 'b' ELSE NULL END AS m2m_ROLE
    ,COUNT(CASE WHEN email_type_id IN (1,2) THEN 1 ELSE NULL END) AS suspicious_contacts
    ,COUNT(email_tracking_id) AS non_purchase_m2m_count 
    FROM  access_views.dw_ue_email_tracking m 
    
    JOIN V_hre_volume e ON e.user_Id = m.sndr_id
            WHERE sndr_id NOT IN (SELECT buyer_id FROM dw_checkout_trans WHERE created_dt >= DATE - 5) AND src_cre_date >= DATE - 5
GROUP BY 1,2 
) WITH DATA PRIMARY INDEX (sndr_id) ON COMMIT PRESERVE ROWS;
CREATE  MULTISET VOLATILE TABLE v_no_trans_m2m_sell , NO FALLBACK ,
NO BEFORE JOURNAL,
NO AFTER JOURNAL,
CHECKSUM = DEFAULT,
DEFAULT MERGEBLOCKRATIO AS(
SELECT sndr_id 
 ,CASE WHEN 1=1 THEN 's' ELSE NULL END AS m2m_ROLE
    ,COUNT(email_tracking_id) AS non_purchase_m2m_count 
    ,COUNT(CASE WHEN email_type_id IN (2) THEN 1 ELSE NULL END) AS suspicious_contacts
    FROM  access_views.dw_ue_email_tracking m 
    JOIN V_hre_volume e ON e.user_Id = m.sndr_id
            WHERE sndr_id NOT IN (SELECT seller_id FROM dw_checkout_trans WHERE created_dt >= DATE - 5) AND src_cre_date >= DATE - 5
GROUP BY 1,2                 
) WITH DATA PRIMARY INDEX (sndr_id) ON COMMIT PRESERVE ROWS;
CREATE  MULTISET VOLATILE TABLE v_all_m2m_attempts , NO FALLBACK ,
NO BEFORE JOURNAL,
NO AFTER JOURNAL,
CHECKSUM = DEFAULT,
DEFAULT MERGEBLOCKRATIO AS(
SELECT v.*
,b.m2m_ROLE AS byr_sndr
,b.Trans_with_M2M AS byr_m2m_trnx_cnt
,b.Trans_with_M2M_Pre_Pay AS byr_pre_pay_m2m
,b.Trans_with_M2M_Post_Pay AS byr_post_pay_m2m
,b.Total_Trans AS byr_m2m_total_trans
,b.M2M_Buyer_Init AS byr_byr_initiated_cnt
,b.M2M_Seller_Init AS byr_slr_initiated_cnt
,bb.non_purchase_m2m_count AS byr_no_tnx_m2m_count 
,bb.suspicious_contacts AS byr_no_susp_contact
,s.m2m_ROLE AS slr_sndr
,s.Trans_with_M2M AS slr_m2m_trnx_cnt
,s.Trans_with_M2M_Pre_Pay AS slr_pre_pay_m2m
,s.Trans_with_M2M_Post_Pay AS slr_post_pay_m2m
,s.Total_Trans AS slr_m2m_total_trans
,s.M2M_Buyer_Init AS slr_byr_initiated_cnt
,s.M2M_Seller_Init AS slr_slr_initiated_cnt
,sc.non_purchase_m2m_count AS slr_no_tnx_m2m_count 
,sc.suspicious_contacts AS slr_no_susp_contact
 FROM V_hre_volume v
    LEFT JOIN v_att_trans_m2m_buy b ON v.user_id = b.user_id 
    LEFT JOIN v_att_trans_m2m_sell s ON v.user_id = s.user_id 
    LEFT JOIN v_no_trans_m2m_buy bb ON v.user_id = bb.sndr_id 
    LEFT JOIN v_no_trans_m2m_sell sc ON v.user_id = sc.sndr_id
) WITH DATA PRIMARY INDEX (user_id) ON COMMIT PRESERVE ROWS;
---______________________________________________________________________________________________________________
    CREATE MULTISET VOLATILE TABLE v_raw_users AS (    
    -- BRM Pre-defined
SELECT DISTINCT usr.user_slctd_id    
,CAST(CHAR_LENGTH(user_slctd_id) AS DECIMAL (18,2)) AS character_count
,REGEXP_REPLACE(REGEXP_REPLACE(user_slctd_id, 'a.' ,'',1,0,'i' ), 'e.' ,'',1,0,'i' ) AS non_vowel_cnt
,REGEXP_REPLACE(REGEXP_REPLACE(non_vowel_cnt, 'i.' ,'',1,0,'i' ), 'o.' ,'',1,0,'i' ) AS non_vowel_cnt1
,REGEXP_REPLACE(REGEXP_REPLACE(non_vowel_cnt1, 'u.' ,'',1,0,'i' ), 'y.' ,'',1,0,'i' ) AS non_vowel_cnt2
,CAST(CHAR_LENGTH(non_vowel_cnt2) AS DECIMAL (18,2)) AS consonant_count
,CAST(consonant_count / character_count AS DECIMAL (18,2))  AS nonvowel_rate
,CASE WHEN consonant_count =  character_count OR nonvowel_rate >= .8  THEN 'x' ELSE NULL END AS likely_gibberish_flag
,usr.email    
,SUBSTR(usr.email, INSTR(usr.email, '@')+1)AS email_domain    
,CASE WHEN email NOT LIKE ALL ('%order%','%support%','sales%','%sales%', '%support%','%order%','%ebay%','%customer%','%support%','%info%')  THEN NULL ELSE 'x' END AS email_whitelist_qualifier
,CASE WHEN email_domain NOT LIKE ALL ('%org', '%.gov','%.edu','%.mil')  THEN NULL ELSE 'x' END AS domain_whitelist_qualifier
,usr.user_id    
,usr.linked_paypal_acct    
,usr.TOP_SLR_LVL_CODE    
,usr.MOTORS_SELLER_LEVEL    
,usr.user_name    
,usr.ADDR     
,(usr.city ||' '|| usr.state ||' '|| usr.PSTL_CODE) AS reg_city_state_zip    
,CNT.CNTRY_DESC AS user_specific_country
,usr.DAYPHONE    
,usr.DATE_OF_BIRTH    
,usr.FEEDBACK_SCORE    
,usr.USER_CRE_DATE    
,last_case
,usr.user_ip_addr     
,SUBSTR(usr.user_ip_addr, 0,8+(POSITION('.' IN SUBSTR(usr.user_ip_addr,9)))) AS ip_address_trunc
,cntr.cntry_desc AS reg_ip_country
,cntry_confidence_val AS ip_cntry_confidence_val
,usr.REG_MCHN_GROUP_ID AS reg_mchn_grp_id
FROM prs_secure_v.dw_users usr     
    JOIN v_all_m2m_attempts r ON r.user_id = usr.user_Id 
       LEFT JOIN dw_countries CNT ON usr.USER_CNTRY_ID = CNT.CNTRY_ID
       LEFT JOIN tns_tables.dw_tns_ip_addr_geo_loc_lkp tns ON ip_address_trunc = tns.ip3_addr
       LEFT JOIN dw_countries cntr ON tns.cntry_name = cntr.cntry_id 
) WITH DATA PRIMARY INDEX ( user_id, EMAIL)    
ON COMMIT PRESERVE ROWS;
CREATE MULTISET VOLATILE TABLE V_issues1 AS (
SEL lst.user_id
,scenario_id
,CASE WHEN scenario_id IN (44,45,50,59,61,94,166,236,261,270,271,272,273,288,319,334) THEN 'x' ELSE NULL END AS potential_identity_qualifier
,CASE WHEN scenario_id = 246 THEN 'x' ELSE NULL END AS possible_cs_contact
,CASE WHEN scenario_id IN (37, 291, 159, 157, 156, 158, 274, 179, 393, 309, 326, 325, 264, 257, 258, 33, 220, 259, 255, 178, 225, 260, 352, 105, 53, 281, 223, 124, 373, 374, 375, 376, 379, 377, 378, 328, 60, 256, 392, 292, 118, 117, 119, 99, 36, 70, 306, 92, 205, 184, 266, 350, 189, 226, 128, 315, 330, 168, 261, 293, 348, 398, 369, 30, 65, 152, 151, 267, 443, 388, 310, 418) THEN 'x' ELSE NULL END AS whitelist_placed
,CASE WHEN scenario_id IN (14, 40,41,42,58,97,111,112,156,157,158,159,160,168,184,221,285,309,318,319, 320, 463,327,362,363,394,417, 423, 424, 425, 426, 428, 431, 432, 466) THEN 'x' ELSE NULL END AS ebay_vetted_likely
,CASE WHEN scenario_id IN (2,3,49,167,193,384,385,335) THEN 'x' ELSE NULL END AS identity_concerns
,CASE WHEN scenario_id IN (457,491,535) THEN 'x' ELSE NULL END AS buyer_fraud_flagged
,CASE WHEN scenario_id IN (445, 446, 447, 448, 449 ) THEN 'x' ELSE NULL END AS zoot_risk
,CASE WHEN scenario_id IN (67) THEN 'x' ELSE NULL END AS us_uk_unsited_user
,CASE WHEN scenario_id IN (123, 172, 233, 248, 250) THEN 'x' ELSE NULL END AS already_restricted
,CASE WHEN scenario_id IN (115,176,186,187,235) THEN 'x' ELSE NULL END AS paypal_restricted
FROM v_raw_users lst 
LEFT JOIN ACCESS_VIEWS.DW_USER_ISSUE iss ON iss.user_id = lst.user_Id
JOIN dw_users usr ON lst.user_id = usr.user_id
QUALIFY ROW_NUMBER() OVER (PARTITION BY lst.user_id , scenario_id ORDER BY iss.SRC_LAST_MODFD_DATE) = 1
) WITH DATA PRIMARY INDEX (user_id, scenario_id)
ON COMMIT PRESERVE ROWS;
CREATE MULTISET VOLATILE TABLE V_issues2 AS (
SELECT 
user_id
,COUNT(potential_identity_qualifier) AS identity_qualifier_test
,COUNT(possible_cs_contact) AS possible_cs_contact_test
,COUNT(whitelist_placed) AS whitelist_placed_test
,COUNT(ebay_vetted_likely) AS ebay_vetted_likely_test
,COUNT(identity_concerns) AS identity_concerns_test
,COUNT(buyer_fraud_flagged) AS buyer_fraud_flagged_test
,COUNT(zoot_risk) AS zoot_risk_test
,COUNT(us_uk_unsited_user) AS us_uk_unsited_user_test
,COUNT(already_restricted) AS already_restricted_test
,COUNT(paypal_restricted) AS paypal_restricted_test
FROM V_issues1
GROUP BY 1
) WITH DATA PRIMARY INDEX (user_id)
ON COMMIT PRESERVE ROWS;
CREATE MULTISET VOLATILE TABLE V_issues3 AS (
SELECT 
user_id
,CASE WHEN identity_qualifier_test = 0 THEN NULL ELSE 'x' END AS identity_qual
,CASE WHEN possible_cs_contact_test = 0 THEN NULL ELSE 'x' END AS poss_cs_contact_qual
,CASE WHEN whitelist_placed_test = 0 THEN NULL ELSE 'x' END AS whitlst_placed_qual
,CASE WHEN ebay_vetted_likely_test = 0 THEN NULL ELSE 'x' END AS ebay_vetted_likely_qual
,CASE WHEN identity_concerns_test = 0 THEN NULL ELSE 'x' END AS identity_concerns_disqual
,CASE WHEN buyer_fraud_flagged_test = 0 THEN NULL ELSE 'x' END AS prev_buyer_fraud_flag
,CASE WHEN zoot_risk_test = 0 THEN NULL ELSE 'x' END AS zoot_risk_disqual
,CASE WHEN us_uk_unsited_user_test = 0 THEN NULL ELSE 'x' END AS us_uk_unsited_user_disqual
,CASE WHEN already_restricted_test = 0 THEN NULL ELSE 'x' END AS already_restricted_disqual
,CASE WHEN paypal_restricted_test = 0 THEN NULL ELSE 'x' END AS paypal_restricted_disqual
FROM V_issues2
) WITH DATA PRIMARY INDEX (user_id)
ON COMMIT PRESERVE ROWS;
CREATE MULTISET VOLATILE TABLE V_brm_buyers1 AS (
SELECT buy.*
,cki.byr_id
,cki.slr_id
,lstg_id AS item_id
,ck_trans_id AS transaction_id
,b.site_id AS trans_site_id
,cat.meta_categ_id
,cki.rprtd_gmv_dt
,CASE
    WHEN PYMT_INSTRMT_TYPE_CD = 1 THEN 'CREDIT CARD'
    WHEN PYMT_INSTRMT_TYPE_CD = 2 THEN 'DIRECT DEBIT'
    WHEN PYMT_INSTRMT_TYPE_CD = 3 THEN 'PayPal'
 end AS instrument_type
 ,CHECKOUT_STATUS_DETAILS 
, CASE
    WHEN PYMT_INSTRMT_RFRNC_TXT LIKE 'FundingInstrumentService:TD_%' THEN 'non-remember-instrument'
    WHEN PYMT_INSTRMT_RFRNC_TXT LIKE 'FundingInstrumentService:TC_%' THEN 'non-remember-instrument'
    WHEN PYMT_INSTRMT_RFRNC_TXT LIKE 'FundingInstrumentService:CC_%' THEN 'remember-instrument'
    WHEN PYMT_INSTRMT_RFRNC_TXT LIKE 'FundingInstrumentService:DD_%' THEN 'remember-instrument'
  end AS remember_or_no
,pymt_amt
,pymt_status_id
,CASE WHEN item_best_offr_id IS NOT NULL THEN 'Y' ELSE 'N' END AS best_offer
,CASE WHEN (((CASE WHEN b.checkout_flags < 0 THEN b.checkout_flags + 9223372036854775808 ELSE b.checkout_flags end) (BIGINT) ) /128 ) MOD 2 =1 THEN 'Y' ELSE 'N' END AS request_total_sent_invoice
,CASE WHEN (((CASE WHEN b.checkout_flags < 0 THEN b.checkout_flags + 9223372036854775808 ELSE b.checkout_flags end) (BIGINT) ) /128 ) MOD 2 =1 THEN 'Y' ELSE 'N' END AS seller_sent_invoice
,CASE WHEN (((CASE WHEN b.checkout_flags < 0 THEN b.checkout_flags + 9223372036854775808 ELSE b.checkout_flags END) (BIGINT) ) /2097152 ) MOD 2 =1 THEN 'Y' ELSE 'N' END AS buyer_marked_as_paid
,CASE WHEN  (BBE.ESC_SNAD_FLAG = 1 OR OPEN_SNAD_FLAG =1 OR RTRN_SNAD_FLAG=1 OR BBE.LOW_DSR_IAD_FLAG = 1 OR BBE.BYR_TO_SLR_NN_FLAG = 1)THEN 1 ELSE 0 END AS SNAD_IAD_NN 
,CASE WHEN ESC_SNAD_FLAG=1  THEN 1 ELSE 0 END AS  SNAD_ESC                                  
,CASE WHEN esc_INR_flag = 1 THEN 1 ELSE 0 END INR_ESC          
,CASE WHEN CNCL_RQST_ID IS NOT NULL THEN 1 ELSE 0 END cancellation_id
,CASE WHEN UPI_YN_IND = 'Y' THEN 1 ELSE 0 END AS byr_upi
,CASE WHEN  BBE.STOCKOUT_FLAG = 1 THEN 1 ELSE 0 END STOCKOUT                                                                                                                                                               
,CASE WHEN  (BBE.STOCKOUT_FLAG = 1  OR CPS_CLAIM_FAULT_TYPE_CD IN (1,6))THEN 1 ELSE 0 END DEFECT                                                                                                                                                                  
,CASE WHEN RTRN_RSN_CD IN (4,5,6,8,9,10,14,15,16) THEN 1 ELSE 0 END AS RTRN_SNAD
,CASE WHEN RTRN_RSN_CD IN (1,2,3,7,11,12,13,17) THEN 1 ELSE 0 END AS RTRN_REMORSE
,NET_LOSS_USD_AMT AS net_loss
,BYR_ATO_YN_IND 
,CASE WHEN CPS_RCNT_CLAIM_ID  IS NOT NULL THEN 1 ELSE 0 END AS claim_COUNT
,CASE WHEN  ( CPS_CLAIM_FAULT_TYPE_CD IN (1,6))THEN 1 ELSE 0 END SAF_CLAIM              
,CASE WHEN  ( CPS_CLAIM_FAULT_TYPE_CD IN (3,5))THEN 1 ELSE 0 END BAF_CLAIM        
,CASE WHEN (((CASE WHEN b.CHECKOUT_FLAGS < 0 THEN b.CHECKOUT_FLAGS + 9223372036854775808 ELSE b.CHECKOUT_FLAGS end) (BIGINT) ) /128 ) MOD 2 =1 THEN 'Y' ELSE 'N' END AS slr_sent_invoice_after_byr_req
,user_incntv_cd
,CPS_CLAIM_AMT 
,SUM(CAST(cki.item_price * cki.qty * cki.lc_exchng_rate AS DECIMAL(18,2))) AS gmb_usd
,SUM(CAST(cki.item_price * cki.qty * cki.lc_exchng_rate / cki.byr_lc_exchng_rate AS DECIMAL(18,2))) AS gmb_buyer_rr_currency
FROM v_raw_users buy
    LEFT JOIN dw_gem2_cmn_ck_i cki ON cki.byr_id = buy.user_id AND COALESCE(cki.RPRTD_WACKO_YN, cki.CK_WACKO_YN) = 'N' /* this handles wacko add-backs */ AND cki.rprtd_gmv_dt >= DATE - 30
    LEFT JOIN PRS_RESTRICTED_V.EBAY_TRANS_RLTD_EVENT BBE ON cki.lstg_id = bbe.item_id AND cki.ck_trans_id = bbe.trans_id AND TRANS_DT  >= DATE - 30
    LEFT JOIN dw_users usr ON usr.user_id = cki.slr_id
    LEFT JOIN dw_checkout_trans b ON cki.lstg_id = b.item_id AND cki.ck_trans_id = b.transaction_id --AND b.created_dt  >= DATE - 90
    LEFT JOIN access_views.DW_MAO_ITEM_BEST_OFFR offr ON offr.byr_id = b.buyer_id AND offr.item_id = b.item_id
    LEFT JOIN DW_PYMT_TRANS a ON a.item_id=b.item_id AND a.transaction_id=b.transaction_id
    LEFT JOIN dw_category_groupings cat ON cat.site_id = b.site_id AND cat.leaf_categ_id = b.leaf_categ_id
    LEFT JOIN prs_restricted_v.dw_eip_redeemed red ON cki.byr_id = buy.user_id AND cki.lstg_id = red.item_id AND cki.ck_trans_id = red.transaction_id AND red.redmd_date  >= DATE - 90 AND incntv_id IN (10036351 ,6342471601 ,10029811 ,10033148 ,10035276 ,10027309 ,10034537 ,10032956 ,10035284 ,10029813 ,10029930 ,6342448001 ,6342449401 ,6342447202 ,10035332 ,10035549 ,10032864 ,10029999 ,10032858 ,6342441301 ,10029845 ,10036269 ,10029196 ,10036366 ,10033154 ,10029359 ,6342451501 ,10036352 ,6342448201 ,10033158 ,10035282 ,10033153 ,10032865 ,10029847 ,10027310 ,10032855 ,10033159 ,10029355 ,6342450901 ,6342471701 ,6342450201 ,10029197 ,6342442501 ,6342450801 ,6342444601 ,10032952 ,10029810 ,10035508 ,10027957 ,10032192 ,10035509 ,10032861 ,6342444801 ,6342478701 ,10029812 ,6342446701 ,10036270 ,10029906 ,10032930 ,6342440601 ,10034538 ,10029907 ,6342432601 ,6342478501 ,10033147 ,10035277 ,10030000 ,10032193 ,10034085 ,6342451001 ,10030538 ,10032853 ,6342444101 ,10029356 ,10033151 ,10031135 ,6342445301 ,10033155 ,10035333 ,10035285 ,10033157 ,10036272 ,10029361 ,10034536 ,10027958 ,6342461601 ,6342451101 ,10035331 ,10029354 ,6342450001 ,10033152 ,10035278 ,6342440701 ,10032909 ,6342441602 ,10036271 ,6342446501 ,6342478601 ,10032859 ,6342450101 ,6342444001 ,10029929 ,6342432802 ,10033150 ,10032852 ,10032194 ,10032190 ,10035695 ,10028202)
GROUP BY 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63
) WITH DATA PRIMARY INDEX (user_id)
    ON COMMIT PRESERVE ROWS;
CREATE MULTISET VOLATILE TABLE V_brm_buyers2 AS (
SELECT b1.*
,NULLIFZERO(SUM(CASE WHEN  meta_categ_id IN (160, 625, 293,619, 172008, 15032, 281, 1249, 11116, 293, 58058, 293)  AND gmb_usd >= 100 THEN 1 ELSE NULL END)) AS  high_risk_purchase              
    FROM V_brm_buyers1  b1
GROUP BY 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65
) WITH DATA PRIMARY INDEX (user_id)
    ON COMMIT PRESERVE ROWS;
CREATE MULTISET VOLATILE TABLE V_aggregated_byr_lst AS (
SELECT b1.*
,identity_qual
,poss_cs_contact_qual
,whitlst_placed_qual
,ebay_vetted_likely_qual
,identity_concerns_disqual
,prev_buyer_fraud_flag
,zoot_risk_disqual
,us_uk_unsited_user_disqual
,already_restricted_disqual
,paypal_restricted_disqual
,COUNT((CASE WHEN v1.item_id IS NOT NULL THEN v1.byr_id ELSE NULL end)) AS purchase_count
,COUNT(CASE WHEN v1.item_id IS NOT NULL AND buyer_marked_as_paid = 'Y' AND pymt_status_id IN (1,3) THEN 1 ELSE NULL end) AS unpaid_markedaspaid
,COUNT(CASE WHEN v1.item_id IS NOT NULL AND best_offer = 'Y' AND pymt_status_id IN (1,3) THEN 1 ELSE NULL end) AS unpaid_bestoffer
,COUNT(CASE WHEN v1.item_id IS NOT NULL AND high_risk_purchase >= 1 THEN 1 ELSE NULL end) AS high_value_purchases
FROM v_raw_users b1
    LEFT JOIN V_issues3 v3 ON b1.user_id = v3.user_id
    LEFT JOIN V_brm_buyers2 v1 ON v1.byr_id = b1.user_id
   
GROUP BY 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40
) WITH DATA PRIMARY INDEX (user_id)
    ON COMMIT PRESERVE ROWS; 
COLLECT STATS ON V_aggregated_byr_lst INDEX (user_id);
COLLECT STATS ON V_aggregated_byr_lst COLUMN (user_id);
CREATE MULTISET VOLATILE TABLE V_aggregated_byr_lst1 AS (
    SELECT lst.*
    ,COUNT(DISTINCT user_rule_actvty_key) AS block_m2m_count 
FROM V_aggregated_byr_lst lst
    JOIN TNS_ACCESS_VIEWS.dw_tns_user_rule_actvty a ON a.user_id = lst.user_id AND RULE_ACTN_ID IN  (35, 40, 42)  AND RULE_FIRE_DT >= DATE -7 AND content_life_cycle_msg_id IN ('AAQ_XB_CCRemedy_4_26_06'
,'Block_message_when_recipient_has_Issue_501'
,'M2M_Contact_Restriction_3day_Issue_553'
,'M2M_Contact_Restriction_indef_Issue_554'
,'M2M_Contact_Restriction_Issue_501'
,'OffEbayFVF_Seller_Block_M2M_EmailPhone'
,'OffEbaySales_Block_M2M_EmailPhone'
,'OffEbaySales_Block_M2M_EmailPhone_shorterformobile'
,'OffEbaySales_Block_M2M_Intentkeywords')
GROUP BY 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44
    
) WITH DATA PRIMARY INDEX (user_id)
    ON COMMIT PRESERVE ROWS; 
    
CREATE MULTISET VOLATILE TABLE V_aggregated_byr_lst2 AS (
    
    SELECT lst.*
,COUNT(CASE WHEN c.seller_id IS NOT NULL THEN 1 ELSE NULL end) AS listed_item  
FROM V_aggregated_byr_lst lst
    JOIN dw_checkout_trans c ON lst.user_id = c.seller_id AND created_dt >= DATE - 90
GROUP BY 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44
) WITH DATA PRIMARY INDEX (user_id)
    ON COMMIT PRESERVE ROWS; 
        CREATE MULTISET VOLATILE TABLE V_aggregated_byr_lst4 AS (
SELECT lst.*
,block_m2m_count
,listed_item
FROM V_aggregated_byr_lst lst
    LEFT JOIN V_aggregated_byr_lst1 lst1 ON lst.user_id = lst1.user_id
    LEFT JOIN V_aggregated_byr_lst2 lst2 ON lst.user_id = lst2.user_id
) WITH DATA PRIMARY INDEX (user_id)
    ON COMMIT PRESERVE ROWS; 
CREATE MULTISET VOLATILE TABLE V_aggregated_byr_lst5 AS (
SELECT l4.*
,CASE WHEN curr.user_id IS NOT NULL THEN 'x' ELSE NULL END AS rvs_yn_flag
,CASE WHEN slr.seller_id IS NOT NULL THEN 'x' ELSE NULL END AS managed_yn_flag
,CASE WHEN MOTORS_SELLER_LEVEL = 2  THEN 'x' ELSE NULL END AS anchor_yn_flag
,CASE WHEN i.user_id IS NOT NULL  THEN 'x' ELSE NULL END AS gc_issue
,CASE WHEN gc_issue IS NOT NULL OR gc_bu_tna_mac IS NOT NULL THEN 'x' ELSE NULL END AS gc_yn_flag
,NULLIFZERO(COUNT(DISTINCT(case_id_user))) AS no_action_counts
,NULLIFZERO(COUNT(DISTINCT(mac.mac_activity_id))) AS appeal_counts
,NULLIFZERO(COUNT(DISTINCT(mac1.mac_activity_id))) AS gc_bu_tna_mac
FROM V_aggregated_byr_lst4 l4
  LEFT JOIN MIDW_ACCESS_VIEWS.F_RVS_USER_SEGM_CURR curr ON curr.user_id = l4.user_id 
  LEFT JOIN APP_SELLER_L2_V.SDM_MDP_BOB_SELLER slr ON slr.seller_id = l4.user_id AND CURR_IND = 'Y'
  LEFT JOIN p_fact_reviews_t.risk_case_volume v ON v.user_id = l4.user_id AND v.severity_lvl = 1  -- No action
  LEFT JOIN Ebay_TS_V.MAC_ACTIVITY MAC ON mac.user_id = l4.user_id AND ((MAC.RULE_ID = 4 AND MAC.DISPOSITION_ID = 1705 AND MAC.ISSUE_ID= 1588)  OR  (MAC.RULE_ID IN (1827,1864,1846) AND MAC.DISPOSITION_ID IN (1631,2931) AND MAC.ISSUE_ID IN (1587,1569)) ) -- appeal 
  LEFT JOIN Ebay_TS_V.MAC_ACTIVITY MAC1 ON mac1.user_id = l4.user_id AND MAC1.RULE_ID = 1747 AND MAC1.DISPOSITION_ID = 2942 AND MAC1.ISSUE_ID= 2883  -- green channel p1 
  LEFT JOIN dw_user_issue i ON i.user_id = l4.user_id AND i.scenario_id IN (319,320,463) AND i.STATUS_ID = 0
GROUP BY 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50
) WITH DATA PRIMARY INDEX (user_id)
    ON COMMIT PRESERVE ROWS; 
CREATE VOLATILE TABLE V_psu_link AS (   
SELECT DISTINCT dtl.user_id
,linked_user_id
,dtl.SRC_CRE_DT AS link_cre_date
,compromised_yn_flag_id AS ATO_flag
,link_prblty AS link_probability
,link_attr_val AS linking_token_value
,CASE WHEN link_attr_name = 'unknown' THEN 'Paypal Referral ID' ELSE link_attr_name END AS link_definition
,CASE WHEN link_definition IN ('IP Address', 'Sign-in IP Address', 'Usage Cookie') THEN 1 WHEN
            link_definition IN ('Phone') THEN 2 WHEN
            link_definition IN ('CC/DD token', 'Invalid Email', 'Paypal Referral ID','Email') THEN 3 ELSE NULL 
                  END AS link_confidence
,usr.psu_type_desc AS user_psu_check
,lsr.psu_type_desc AS linked_user_psu_check
FROM tns_access_views.dw_tns_fc_user_link_dtl dtl 
JOIN V_aggregated_byr_lst5 lst ON lst.user_id = dtl.user_id
JOIN prs_secure_v.DW_USERS_INFO_FREQ us ON us.user_id = linked_user_id AND us.user_sts_code = 0
JOIN tns_access_views.DW_TNS_FC_LKP_LINK_ATTR_MAP mp ON mp.link_attr_map_id = dtl.link_attr_map_id
JOIN tns_access_views.DW_TNS_FC_LKP_LINK_ATTR ATT ON mp.link_attr_id = att.link_attr_id
LEFT JOIN tns_access_views.DW_TNS_FC_LKP_PSU_TYPE usr ON dtl.user_psu_type_id = usr.psu_type_id
LEFT JOIN tns_access_views.DW_TNS_FC_LKP_PSU_TYPE lsr ON dtl.linked_user_psu_type_id = lsr.psu_type_id
WHERE link_definition NOT IN ('Time diff between reg', 'ZIP', 'Time difference between sign-ins') AND link_cre_date >= DATE - 365 AND link_confidence IN (2,3)
QUALIFY ROW_NUMBER() OVER(PARTITION BY dtl.user_id || dtl.linked_user_Id ORDER BY link_probability DESC) =1                                
                                                                             
) WITH DATA PRIMARY INDEX (user_id )                                                              
ON COMMIT PRESERVE ROWS;
CREATE MULTISET VOLATILE TABLE V_aggregated_byr_lst6 AS (
        
    SELECT b1.*, COUNT(freq.item_id) AS freq_bids FROM V_aggregated_byr_lst5 b1
        JOIN dw_bid_freq freq ON freq.bdr_id = b1.user_id AND cre_date BETWEEN DATE - 4 AND DATE - 2
        
GROUP BY 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49
) WITH DATA PRIMARY INDEX (user_id)
    ON COMMIT PRESERVE ROWS;
DROP TABLE V_aggregated_byr_lst8
CREATE MULTISET VOLATILE TABLE V_aggregated_byr_lst8 AS (
SELECT r1.*
,us.user_sts_code
,NULLIFZERO(COUNT(psu.linked_user_id)) AS psu_links
,CASE WHEN r1.email_whitelist_qualifier IS NOT NULL THEN 1
            WHEN r1.domain_whitelist_qualifier IS NOT NULL THEN 1
            WHEN r1.FEEDBACK_SCORE >= 6 THEN 1        
            WHEN r1.appeal_counts IS NOT NULL THEN 1 
            WHEN r1.gc_yn_flag IS NOT NULL THEN 1 
            WHEN r1.managed_yn_flag IS NOT NULL THEN 1 
            WHEN r1.rvs_yn_flag IS NOT NULL THEN 1 
            WHEN r1.anchor_yn_flag IS NOT NULL THEN 1 
            WHEN r1.no_action_counts IS NOT NULL THEN 1 
            WHEN r1.listed_item >=1 THEN 1
            WHEN r1.user_cre_date <= DATE - 180 THEN 1
            WHEN us.user_sts_code = 0 THEN 4 
ELSE NULL END AS confidence_score
FROM V_aggregated_byr_lst5 r1
    LEFT JOIN  V_psu_link psu ON psu.user_id = r1.user_id
    LEFT JOIN prs_secure_v.DW_USERS us ON us.user_id = r1.user_id
GROUP BY 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55
) WITH DATA PRIMARY INDEX (user_id)
    ON COMMIT PRESERVE ROWS;
COLLECT STATISTICS COLUMN (USER_ID) ON V_aggregated_byr_lst8;
CREATE MULTISET VOLATILE TABLE V_final_result AS (
    SELECT USER_SLCTD_ID
    ,character_count
    ,non_vowel_cnt
    ,non_vowel_cnt1
    ,non_vowel_cnt2
    ,consonant_count
    ,nonvowel_rate
    ,likely_gibberish_flag
    ,EMAIL
    ,email_domain
    ,email_whitelist_qualifier
    ,domain_whitelist_qualifier
    ,l8.USER_ID
    ,LINKED_PAYPAL_ACCT
    ,TOP_SLR_LVL_CODE
    ,MOTORS_SELLER_LEVEL
    ,USER_NAME
    ,ADDR
    ,reg_city_state_zip
    ,user_specific_country
    ,DAYPHONE
    ,DATE_OF_BIRTH
    ,FEEDBACK_SCORE
    ,USER_CRE_DATE
    ,last_case
    ,USER_IP_ADDR
    ,ip_address_trunc
    ,reg_ip_country
    ,ip_cntry_confidence_val
    ,reg_mchn_grp_id
    ,identity_qual
    ,poss_cs_contact_qual
    ,whitlst_placed_qual
    ,ebay_vetted_likely_qual
    ,identity_concerns_disqual
    ,prev_buyer_fraud_flag
    ,zoot_risk_disqual
    ,us_uk_unsited_user_disqual
    ,already_restricted_disqual
    ,paypal_restricted_disqual
    ,purchase_count
    ,high_value_purchases
    ,unpaid_markedaspaid
    ,unpaid_bestoffer
    ,listed_item
    ,rvs_yn_flag
    ,managed_yn_flag
    ,anchor_yn_flag
    ,USER_STS_CODE
    ,psu_links
    ,confidence_score
     FROM V_aggregated_byr_lst8 l8
) WITH DATA PRIMARY INDEX (user_id, user_cre_date)
    ON COMMIT PRESERVE ROWS;
    CREATE MULTISET VOLATILE TABLE V_final_result1 AS (
SELECT  usr.USER_SLCTD_ID
,v.USER_ID
,v.USER_STS_CODE
,v.character_count
,v.nonvowel_rate
,v.likely_gibberish_flag
,v.EMAIL
,v.email_domain
,CASE WHEN raw.user_id IS NOT NULL AND v.email_domain = 'gmail.com' AND strip_count >= 4 THEN 'x' ELSE NULL END AS alias_email_flag
,v.LINKED_PAYPAL_ACCT
,CASE WHEN v.TOP_SLR_LVL_CODE = (-999) THEN NULL ELSE v.TOP_SLR_LVL_CODE END AS top_slr_level
,NULLIFZERO(v.MOTORS_SELLER_LEVEL) AS specialty_seller_level
,v.FEEDBACK_SCORE
,v.DAYPHONE
,v.USER_NAME
,v.ADDR
,v.reg_city_state_zip
,v.user_specific_country
,USER_SITE_ID AS reg_site_id
,v.DATE_OF_BIRTH
,v.USER_CRE_DATE
,v.last_case
,v.USER_IP_ADDR
,v.ip_address_trunc
,v.reg_ip_country
,v.ip_cntry_confidence_val
,v.reg_mchn_grp_id
,v.reg_mchn_grp_id AS last_mchn_grp_id
,NULLIFZERO(v.purchase_count) AS total_purchase
,NULLIFZERO(high_value_purchases) AS high_value_trans_count
,NULLIFZERO(COUNT(CASE WHEN trans_site_id <> USER_SITE_ID THEN 1 ELSE NULL end)) AS mismatching_byr_site_cnt
,NULLIFZERO(COUNT(CASE WHEN request_total_sent_invoice IS NOT NULL THEN 'x' ELSE NULL end)) AS non_null_request_invoice_cnt
,NULLIFZERO(v.unpaid_markedaspaid) AS total_markedaspaid
,NULLIFZERO(v.unpaid_bestoffer) AS total_denied_bestoffer
,NULLIFZERO(v.listed_item) AS items_listed_as_seller
,NULLIFZERO(v.psu_links) AS total_psu_count
,CASE WHEN user_incntv_cd IS NOT NULL THEN 1 ELSE NULL END AS cpn_redeemed_yn
,v.confidence_score
    FROM V_final_result v
LEFT JOIN dw_users usr ON usr.user_id = v.user_id
LEFT JOIN V_brm_buyers2 b1 ON v.user_id = b1.byr_id
LEFT JOIN p_fact_reviews_t.alias_email_volume_raw raw ON v.user_id = raw.user_id
GROUP BY 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,33,34,35,36,37,38
) WITH DATA PRIMARY INDEX (user_id)
    ON COMMIT PRESERVE ROWS;
    CREATE MULTISET VOLATILE TABLE V_final_result2 AS (
 
SELECT USER_SLCTD_ID
,v.USER_ID
,USER_STS_CODE
,character_count
,nonvowel_rate
,likely_gibberish_flag
,EMAIL
,email_domain
,alias_email_flag
,LINKED_PAYPAL_ACCT
,top_slr_level
,specialty_seller_level
,FEEDBACK_SCORE
,DAYPHONE
,USER_NAME
,ADDR
,reg_city_state_zip
,user_specific_country
,reg_site_id
,DATE_OF_BIRTH
,USER_CRE_DATE
,v.last_case
,USER_IP_ADDR
,ip_address_trunc
,reg_ip_country
,ip_cntry_confidence_val
,reg_mchn_grp_id
,last_mchn_grp_id
,total_purchase
,high_value_trans_count
,mismatching_byr_site_cnt
,non_null_request_invoice_cnt
,total_markedaspaid
,total_denied_bestoffer
,items_listed_as_seller
,total_psu_count
,cpn_redeemed_yn
,confidence_score
,byr_sndr
,byr_m2m_trnx_cnt
,byr_pre_pay_m2m
,byr_post_pay_m2m
,byr_m2m_total_trans
,byr_byr_initiated_cnt
,byr_slr_initiated_cnt
,byr_no_tnx_m2m_count
,byr_no_susp_contact
,slr_sndr
,slr_m2m_trnx_cnt
,slr_pre_pay_m2m
,slr_post_pay_m2m
,slr_m2m_total_trans
,slr_byr_initiated_cnt
,slr_slr_initiated_cnt
,slr_no_tnx_m2m_count
,slr_no_susp_contact
 FROM V_final_result1 v
    LEFT JOIN v_all_m2m_attempts s ON s.user_id = v.user_id
    
    ) WITH DATA PRIMARY INDEX (user_id)
    ON COMMIT PRESERVE ROWS;
DELETE FROM p_fact_reviews_t.hre_daily_batch;
MERGE INTO p_fact_reviews_t.hre_daily_batch wow USING V_final_result2 v3 
ON  v3.user_id = wow.user_id
WHEN NOT MATCHED THEN INSERT VALUES (
user_slctd_id = v3.user_slctd_id
,USER_ID = v3.USER_ID
,USER_STS_CODE = v3.USER_STS_CODE
,character_count = v3.character_count
,nonvowel_rate = v3.nonvowel_rate
,likely_gibberish_flag = v3.likely_gibberish_flag
,EMAIL = v3.EMAIL
,email_domain = v3.email_domain
,alias_email_flag = v3.alias_email_flag
,LINKED_PAYPAL_ACCT = v3.LINKED_PAYPAL_ACCT
,top_slr_level = v3.top_slr_level
,specialty_seller_level = v3.specialty_seller_level
,FEEDBACK_SCORE = v3.FEEDBACK_SCORE
,DAYPHONE = v3.DAYPHONE
,USER_NAME = v3.USER_NAME
,ADDR = v3.ADDR
,reg_city_state_zip = v3.reg_city_state_zip
,user_specific_country = v3.user_specific_country
,reg_site_id = v3.reg_site_id
,DATE_OF_BIRTH = v3.DATE_OF_BIRTH
,USER_CRE_DATE = v3.USER_CRE_DATE
,last_case = v3.last_case
,USER_IP_ADDR = v3.USER_IP_ADDR
,ip_address_trunc = v3.ip_address_trunc
,reg_ip_country = v3.reg_ip_country
,ip_cntry_confidence_val = v3.ip_cntry_confidence_val
,reg_mchn_grp_id = v3.reg_mchn_grp_id
,last_mchn_grp_id = v3.last_mchn_grp_id
,total_purchase = v3.total_purchase
,high_value_trans_count = v3.high_value_trans_count
,mismatching_byr_site_cnt = v3.mismatching_byr_site_cnt
,non_null_request_invoice_cnt = v3.non_null_request_invoice_cnt
,total_markedaspaid = v3.total_markedaspaid
,total_denied_bestoffer = v3.total_denied_bestoffer
,items_listed_as_seller = v3.items_listed_as_seller
,total_psu_count = v3.total_psu_count
,cpn_redeemed_yn = v3.cpn_redeemed_yn
,confidence_score = v3.confidence_score
,byr_sndr = v3.byr_sndr
,byr_m2m_trnx_cnt = v3.byr_m2m_trnx_cnt
,byr_pre_pay_m2m = v3.byr_pre_pay_m2m
,byr_post_pay_m2m = v3.byr_post_pay_m2m
,byr_m2m_total_trans = v3.byr_m2m_total_trans
,byr_byr_initiated_cnt = v3.byr_byr_initiated_cnt
,byr_slr_initiated_cnt = v3.byr_slr_initiated_cnt
,byr_no_tnx_m2m_count = v3.byr_no_tnx_m2m_count
,byr_no_susp_contact = v3.byr_no_susp_contact
,slr_sndr = v3.slr_sndr
,slr_m2m_trnx_cnt = v3.slr_m2m_trnx_cnt
,slr_pre_pay_m2m = v3.slr_pre_pay_m2m
,slr_post_pay_m2m = v3.slr_post_pay_m2m
,slr_m2m_total_trans = v3.slr_m2m_total_trans
,slr_byr_initiated_cnt = v3.slr_byr_initiated_cnt
,slr_slr_initiated_cnt = v3.slr_slr_initiated_cnt
,slr_no_tnx_m2m_count = v3.slr_no_tnx_m2m_count
,slr_no_susp_contact = v3.slr_no_susp_contact
);