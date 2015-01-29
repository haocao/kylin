package com.kylinolap.job.common;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.hbase.util.Pair;

import com.google.common.collect.Maps;
import com.kylinolap.common.util.Logger;
import com.kylinolap.job.constant.ExecutableConstants;
import com.kylinolap.job.exception.ExecuteException;
import com.kylinolap.job.execution.AbstractExecutable;
import com.kylinolap.job.execution.ExecutableContext;
import com.kylinolap.job.execution.ExecuteResult;

/**
 * Created by qianzhou on 12/26/14.
 */
public class ShellExecutable extends AbstractExecutable {

    private static final String CMD = "cmd";

    public ShellExecutable() {
        super();
    }

    @Override
    protected ExecuteResult doWork(ExecutableContext context) throws ExecuteException {
        try {
            logger.info("executing:" + getCmd());
            final ShellExecutableLogger logger = new ShellExecutableLogger();
            final Pair<Integer, String> result = context.getConfig().getCliCommandExecutor().execute(getCmd(), logger);
            executableManager.addJobInfo(getId(), logger.getInfo());
            return new ExecuteResult(result.getFirst() == 0? ExecuteResult.State.SUCCEED: ExecuteResult.State.FAILED, result.getSecond());
        } catch (IOException e) {
            logger.error("job:" + getId() + " execute finished with exception", e);
            return new ExecuteResult(ExecuteResult.State.ERROR, e.getLocalizedMessage());
        }
    }

    public void setCmd(String cmd) {
        setParam(CMD, cmd);
    }

    public String getCmd() {
        return getParam(CMD);
    }

    private static class ShellExecutableLogger implements Logger {

        private final Map<String, String> info = Maps.newHashMap();

        private static final Pattern PATTERN_APP_ID = Pattern.compile("Submitted application (.*?) to ResourceManager");
        private static final Pattern PATTERN_APP_URL = Pattern.compile("The url to track the job: (.*)");
        private static final Pattern PATTERN_JOB_ID = Pattern.compile("Running job: (.*)");
        private static final Pattern PATTERN_HDFS_BYTES_WRITTEN = Pattern.compile("HDFS: Number of bytes written=(\\d+)");
        private static final Pattern PATTERN_SOURCE_RECORDS_COUNT = Pattern.compile("Map input records=(\\d+)");
        private static final Pattern PATTERN_SOURCE_RECORDS_SIZE = Pattern.compile("HDFS Read: (\\d+) HDFS Write");

        // hive
        private static final Pattern PATTERN_HIVE_APP_ID_URL = Pattern.compile("Starting Job = (.*?), Tracking URL = (.*)");
        private static final Pattern PATTERN_HIVE_BYTES_WRITTEN = Pattern.compile("HDFS Read: (\\d+) HDFS Write: (\\d+) SUCCESS");

        @Override
        public void log(String message) {
            Matcher matcher = PATTERN_APP_ID.matcher(message);
            if (matcher.find()) {
                String appId = matcher.group(1);
                info.put(ExecutableConstants.YARN_APP_ID, appId);
            }

            matcher = PATTERN_APP_URL.matcher(message);
            if (matcher.find()) {
                String appTrackingUrl = matcher.group(1);
                info.put(ExecutableConstants.YARN_APP_URL, appTrackingUrl);
            }

            matcher = PATTERN_JOB_ID.matcher(message);
            if (matcher.find()) {
                String mrJobID = matcher.group(1);
                info.put(ExecutableConstants.MR_JOB_ID, mrJobID);
            }

            matcher = PATTERN_HDFS_BYTES_WRITTEN.matcher(message);
            if (matcher.find()) {
                String hdfsWritten = matcher.group(1);
                info.put(ExecutableConstants.HDFS_BYTES_WRITTEN, hdfsWritten);
            }

            matcher = PATTERN_SOURCE_RECORDS_COUNT.matcher(message);
            if (matcher.find()) {
                String sourceCount = matcher.group(1);
                info.put(ExecutableConstants.SOURCE_RECORDS_COUNT, sourceCount);
            }

            matcher = PATTERN_SOURCE_RECORDS_SIZE.matcher(message);
            if (matcher.find()) {
                String sourceSize = matcher.group(1);
                info.put(ExecutableConstants.SOURCE_RECORDS_SIZE, sourceSize);
            }

            // hive
            matcher = PATTERN_HIVE_APP_ID_URL.matcher(message);
            if (matcher.find()) {
                String jobId = matcher.group(1);
                String trackingUrl = matcher.group(2);
                info.put(ExecutableConstants.MR_JOB_ID, jobId);
                info.put(ExecutableConstants.YARN_APP_URL, trackingUrl);
            }

            matcher = PATTERN_HIVE_BYTES_WRITTEN.matcher(message);
            if (matcher.find()) {
                // String hdfsRead = matcher.group(1);
                String hdfsWritten = matcher.group(2);
                info.put(ExecutableConstants.HDFS_BYTES_WRITTEN, hdfsWritten);
            }
        }

        Map<String, String> getInfo() {
            return info;
        }
    }

}
