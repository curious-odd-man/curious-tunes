package com.github.curiousoddman.curious_tunes.domain;

import lombok.extern.slf4j.Slf4j;
import org.jooq.ExecuteContext;
import org.jooq.ExecuteListener;
import org.jooq.SQLDialect;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

@Slf4j
@Component
public class ExceptionTranslator implements ExecuteListener {
    @Override
    public void exception(ExecuteContext ctx) {
        SQLException sqlException = ctx.sqlException();

        // jOOQ exceptions (e.g. NoDataFoundException) have no underlying SQLException
        if (sqlException == null) {
            RuntimeException cause = ctx.exception();
            if (cause != null) {
                throw cause;
            }
            throw new IllegalStateException("jOOQ reported an error without an exception");
        }

        SQLDialect dialect = ctx.configuration().dialect();
        SQLExceptionTranslator translator = new SQLErrorCodeSQLExceptionTranslator(dialect.name());
        DataAccessException jooqException = translator.translate("jOOQ", ctx.sql(), sqlException);
        if (jooqException != null) {
            throw jooqException;
        }
        log.error("Uknown unmatched error occurred {}", ctx.sql());
    }
}