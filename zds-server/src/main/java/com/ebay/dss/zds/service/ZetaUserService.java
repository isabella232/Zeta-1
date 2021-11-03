package com.ebay.dss.zds.service;

import com.ebay.dss.zds.common.RSACipher;
import com.ebay.dss.zds.dao.ZetaUserRepository;
import com.ebay.dss.zds.exception.ApplicationBaseException;
import com.ebay.dss.zds.exception.ErrorCode;
import com.ebay.dss.zds.exception.InformationManipulationException;
import com.ebay.dss.zds.model.ZetaUser;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class ZetaUserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZetaUserService.class);

    private final ZetaUserRepository zetaUserRepository;
    private final RSACipher rsaCipher;

    @Autowired
    public ZetaUserService(ZetaUserRepository zetaUserRepository, RSACipher rsaCipher) {
        this.zetaUserRepository = zetaUserRepository;
        this.rsaCipher = rsaCipher;
    }


    public ZetaUser getUser(String nt) {
        ZetaUser user = zetaUserRepository.getUser(nt);
        decryptField(user, "githubToken");
        decryptField(user, "tdPass");
        decryptField(user, "windowsAutoAccountPass");
        return user;
    }

    private ZetaUser decryptField(ZetaUser user, String fieldName) {
        try {
            String fieldNameCapitalized = StringUtils.capitalize(fieldName);
            String getMethodName = "get" + fieldNameCapitalized;
            String setMethodName = "set" + fieldNameCapitalized;
            Method getMethod = user.getClass().getMethod(getMethodName);
            Method setMethod = user.getClass().getMethod(setMethodName, String.class);
            String secretMessage = (String) getMethod.invoke(user);

            String plainMessage = null;
            if (StringUtils.isNotBlank(secretMessage)) {
                plainMessage = rsaCipher.decrypt(secretMessage);
            }
            setMethod.invoke(user, plainMessage);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new InformationManipulationException("zeta user", "decrypt field " + fieldName, e);
        }
        return user;
    }

    public String getGithubToken(String nt) {
        ZetaUser user = zetaUserRepository.getUser(nt);
        try {
            decryptField(user, "githubToken");
        } catch (Exception e) {
            LOGGER.error("Get github token error", e);
            user.setGithubToken(null);
        }
        return user.getGithubToken();
    }

    @Transactional
    public ZetaUser saveUserWithAllowedFields(ZetaUser to) {
        ZetaUser old = zetaUserRepository.getUser(to.getNt());
        old.setName(to.getName());
        old.setPreference(to.getPreference());
        old.setTdPass(Objects.nonNull(to.getTdPass()) ? rsaCipher.encrypt(to.getTdPass()) : null);
        old.setWindowsAutoAccountPass(Objects.nonNull(to.getWindowsAutoAccountPass()) ? rsaCipher.encrypt(to.getWindowsAutoAccountPass()) : null);
        old.setGithubToken(Objects.nonNull(to.getGithubToken()) ? rsaCipher.encrypt(to.getGithubToken()) : null);
        old.setUpdateDt(new Timestamp(Instant.now().getEpochSecond()));
        zetaUserRepository.updateUser(old);
        return getUser(to.getNt());
    }

    @Transactional
    public ZetaUser getOrCreateUser(String nt) {
        ZetaUser zetaUser;

        try {
            zetaUser = zetaUserRepository.getUser(nt);
        } catch (IncorrectResultSizeDataAccessException e) {
            LOGGER.info(nt + " first time login, creating user");
            zetaUser = defaultZetaUser(nt);
            zetaUserRepository.addUser(zetaUser);
            zetaUser = zetaUserRepository.getUser(nt);
            // add by tianrui
            // batch alation queries when create new user
            // TODO to thread pool
//            new Thread(() -> alationIntegrationService.batchQueryByNt(nt)).start();

            LOGGER.info(nt + " is now a zeta user");
        }
        return zetaUser;
    }

    public void updateToken(String nt, String token) {
        if (!zetaUserRepository.updateToken(nt, token)) {
            LOGGER.error("Persist user {} token error", nt);
            throw new ApplicationBaseException(ErrorCode.UNAUTHORIZED, "User token cannot be persist.");
        }
    }

    public List<String> getUsers() {
        return zetaUserRepository.getAllUsers();
    }

    public List<String> getUnbatchAlationUsers() {
        return zetaUserRepository.getUnbatchAlationUsers();
    }

    private ZetaUser defaultZetaUser(String nt) {
        // fulfill more default values if entity add fields
        ZetaUser zetaUser = new ZetaUser();
        zetaUser.setNt(nt);
        zetaUser.setName(nt);
        return zetaUser;
    }

}
