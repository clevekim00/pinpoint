package com.nhn.pinpoint.profiler.modifier.db.interceptor;

import java.sql.Connection;

import com.nhn.pinpoint.bootstrap.interceptor.ByteCodeMethodDescriptorSupport;
import com.nhn.pinpoint.bootstrap.interceptor.MethodDescriptor;
import com.nhn.pinpoint.bootstrap.interceptor.SimpleAroundInterceptor;
import com.nhn.pinpoint.bootstrap.interceptor.TraceContextSupport;
import com.nhn.pinpoint.bootstrap.interceptor.tracevalue.DatabaseInfoTraceValue;
import com.nhn.pinpoint.bootstrap.logging.PLogger;

import com.nhn.pinpoint.bootstrap.context.Trace;
import com.nhn.pinpoint.bootstrap.context.TraceContext;
import com.nhn.pinpoint.bootstrap.logging.PLoggerFactory;
import com.nhn.pinpoint.bootstrap.context.DatabaseInfo;

/**
 * @author emeroad
 */
public class TransactionSetAutoCommitInterceptor implements SimpleAroundInterceptor, ByteCodeMethodDescriptorSupport, TraceContextSupport {

    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private MethodDescriptor descriptor;
    private TraceContext traceContext;

    public TransactionSetAutoCommitInterceptor() {
    }

    @Override
    public void before(Object target, Object[] args) {
        if (isDebug) {
            logger.beforeInterceptor(target, args);
        }

        Trace trace = traceContext.currentTraceObject();
        if (trace == null) {
            return;
        }
        if (target instanceof Connection) {
            Connection con = (Connection) target;
            beforeStartTransaction(trace, con);
        }
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (isDebug) {
            logger.afterInterceptor(target, args, result);
        }

        Trace trace = traceContext.currentTraceObject();
        if (trace == null) {
            return;
        }
        if (target instanceof Connection) {
            Connection con = (Connection) target;
            afterStartTransaction(trace, con, args, throwable);
        }
    }

    private void beforeStartTransaction(Trace trace, Connection target) {
        trace.traceBlockBegin();
        trace.markBeforeTime();

    }

    private void afterStartTransaction(Trace trace, Connection target, Object[] arg, Throwable throwable) {
        try {
            DatabaseInfo databaseInfo = null;
            if (target instanceof DatabaseInfoTraceValue) {
                databaseInfo = ((DatabaseInfoTraceValue) target).__getTraceDatabaseInfo();
            }
            if (databaseInfo == null) {
                databaseInfo = UnKnownDatabaseInfo.INSTANCE;
            }
            trace.recordServiceType(databaseInfo.getType());
            trace.recordEndPoint(databaseInfo.getMultipleHost());
            trace.recordDestinationId(databaseInfo.getDatabaseId());


            trace.recordApi(descriptor, arg);
            trace.recordException(throwable);

            trace.markAfterTime();
        } catch (Exception e) {
            if (logger.isWarnEnabled()) {
                logger.warn(e.getMessage(), e);
            }
        } finally {
            trace.traceBlockEnd();
        }
    }


    @Override
    public void setMethodDescriptor(MethodDescriptor descriptor) {
        this.descriptor = descriptor;
        traceContext.cacheApi(descriptor);
    }


    @Override
    public void setTraceContext(TraceContext traceContext) {
        this.traceContext = traceContext;
    }
}
