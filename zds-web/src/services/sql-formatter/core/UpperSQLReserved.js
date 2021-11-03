import tokenTypes from "./tokenTypes";

export default class UpperSQLReserved {
    /**
     * @param {Object} cfg
     *   @param {Object} cfg.indent
     *   @param {Object} cfg.params
     * @param {Tokenizer} tokenizer
     */
    constructor(cfg, tokenizer) {
        this.cfg = cfg || {};
        this.tokenizer = tokenizer;
        this.previousReservedWord = {};
    }

    /**
     * Formats whitespaces in a SQL string to make it easier to read.
     *
     * @param {String} query The SQL query string
     * @return {String} formatted query
     */
    format(query) {
        const tokens = this.tokenizer.tokenize(query);
        const formattedQuery = this.getFormattedQueryFromTokens(tokens);

        return formattedQuery;
    }

    getFormattedQueryFromTokens(tokens) {
        let formattedQuery = "";

        tokens.forEach((token, index) => {
            if (token.type === tokenTypes.RESERVED_TOPLEVEL) {
                formattedQuery = this.formatToplevelReservedWord(token, formattedQuery);
                this.previousReservedWord = token;
            }
            else if (token.type === tokenTypes.RESERVED_NEWLINE) {
                formattedQuery = this.formatNewlineReservedWord(token, formattedQuery);
                this.previousReservedWord = token;
            }
            else if (token.type === tokenTypes.RESERVED) {
              /**
               * comments by huhan @2020-10-13
               * upper case reservedWord
               */
                formattedQuery = this.formatReservedWord(token, formattedQuery);
                this.previousReservedWord = token;
            }
            else {
              formattedQuery += token.value;
          }
        });
        return formattedQuery;
    }


    formatToplevelReservedWord(token, query) {
        /**
         * comments by huhan @2020-10-13
         * upper case reservedWord
         */
        let value = token.value.toUpperCase();
        query += value;
        return query;
    }

    formatNewlineReservedWord(token, query) {
      /**
       * comments by huhan @2020-10-13
       * upper case reservedWord
       */
      let value = token.value.toUpperCase();
        return query + value;
    }
    /**
     * comments by huhan @2020-10-13
     * upper case reservedWord
     */
    formatReservedWord(token, query) {
      let value = token.value.toUpperCase();
        return query + value;
    }
}
