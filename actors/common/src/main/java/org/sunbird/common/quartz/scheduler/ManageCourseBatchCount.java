/** */
package org.sunbird.common.quartz.scheduler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.learner.actors.coursebatch.CourseEnrollmentActor;
import org.sunbird.learner.util.CourseBatchSchedulerUtil;
import org.sunbird.learner.util.Util;
import org.sunbird.telemetry.util.TelemetryUtil;

/**
 * This class will update course batch count in EKStep.
 *
 * @author Manzarul
 */
public class ManageCourseBatchCount implements Job {

  private Map<String, Map<String, Object>> contentMap = new HashMap<>();
  private Map<String, List<Map<String, Object>>> openBatchMap = new HashMap<>();
  private Map<String, List<Map<String, Object>>> privateBatchMap = new HashMap<>();

  @SuppressWarnings("unchecked")
  public void execute(JobExecutionContext ctx) throws JobExecutionException {
    ProjectLogger.log(
        "Executing COURSE_BATCH_COUNT job at: "
            + Calendar.getInstance().getTime()
            + " triggered by: "
            + ctx.getJobDetail().toString());
    Util.initializeContextForSchedulerJob(
        JsonKey.SYSTEM, ctx.getFireInstanceId(), JsonKey.SCHEDULER_JOB);
    Map<String, Object> logInfo =
        genarateLogInfo(JsonKey.SYSTEM, ctx.getJobDetail().getDescription());
    logInfo.put("LOG_LEVEL", "info");
    // Collect all those batches from ES whose start date is today and
    // countIncrementStatus value is
    // false.
    // and all those batches whose end date was yesterday and countDecrementStatus
    // value is false.
    // update the countIncrement or decrement status value as true , countIncrement
    // or decrement
    // date as today date.
    // make the status in course batch table based on course start or end - in case
    // of start make it
    // 1 , for end make it 2.
    // now update the data into cassandra and ES both and EKStep content with count
    // increment and
    // decrement value.
    SimpleDateFormat format = new SimpleDateFormat(ProjectUtil.YEAR_MONTH_DATE_FORMAT);
    Calendar cal = Calendar.getInstance();
    String today = "";
    String yesterDay = "";
    today = format.format(cal.getTime());
    cal.add(Calendar.DATE, -1);
    yesterDay = format.format(cal.getTime());
    ProjectLogger.log(
        "start date and end date is ==" + today + "  " + yesterDay, LoggerEnum.INFO.name());
    Map<String, Object> data = CourseBatchSchedulerUtil.getBatchDetailsFromES(today, yesterDay);
    if (data != null && data.size() > 0) {
      if (null != data.get(JsonKey.START_DATE)) {
        List<Map<String, Object>> listMap =
            (List<Map<String, Object>>) data.get(JsonKey.START_DATE);
        for (Map<String, Object> map : listMap) {
          updateBatchMap(map);
        }
        getContentAndStore(openBatchMap);
        getContentAndStore(privateBatchMap);
        contentMapUpdate(true);
        openBatchMap.clear();
        privateBatchMap.clear();
      }
      if (null != data.get(JsonKey.END_DATE)) {
        List<Map<String, Object>> listMap = (List<Map<String, Object>>) data.get(JsonKey.END_DATE);
        for (Map<String, Object> map : listMap) {
          updateBatchMap(map);
        }
      }
      getContentAndStore(openBatchMap);
      getContentAndStore(privateBatchMap);
      contentMapUpdate(false);
      openBatchMap.clear();
      privateBatchMap.clear();
      if (null != data.get(JsonKey.STATUS)) {
        List<Map<String, Object>> listMap = (List<Map<String, Object>>) data.get(JsonKey.STATUS);
        for (Map<String, Object> map : listMap) {
          updateCourseBatchMap(false, false, map);
          boolean flag = CourseBatchSchedulerUtil.updateDataIntoES(map);
          if (flag) {
            CourseBatchSchedulerUtil.updateDataIntoCassandra(map);
          }
        }
      }
    } else {
      ProjectLogger.log(
          "No data found in Elasticsearch for course batch update.", LoggerEnum.INFO.name());
    }
    TelemetryUtil.telemetryProcessingCall(logInfo, null, null, "LOG");
  }

  private Map<String, Object> genarateLogInfo(String logType, String message) {

    Map<String, Object> info = new HashMap<>();
    info.put(JsonKey.LOG_TYPE, logType);
    long startTime = System.currentTimeMillis();
    info.put(JsonKey.START_TIME, startTime);
    info.put(JsonKey.MESSAGE, message);
    info.put(JsonKey.LOG_LEVEL, JsonKey.INFO);

    return info;
  }

  private void updateCourseBatchMap(
      boolean isCountIncrementStatus,
      boolean isCountDecrementStatus,
      Map<String, Object> courseBatchMap) {
    String todayDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    courseBatchMap.put(JsonKey.STATUS, ProjectUtil.ProgressStatus.STARTED.getValue());
    if (isCountIncrementStatus) {
      courseBatchMap.put(JsonKey.COUNTER_INCREMENT_STATUS, true);
      courseBatchMap.put(JsonKey.COUNT_INCREMENT_DATE, todayDate);
    }
    if (isCountDecrementStatus) {
      courseBatchMap.put(JsonKey.COUNTER_DECREMENT_STATUS, true);
      courseBatchMap.put(JsonKey.COUNT_DECREMENT_DATE, todayDate);
      courseBatchMap.put(JsonKey.STATUS, ProjectUtil.ProgressStatus.COMPLETED.getValue());
    }
    courseBatchMap.put(JsonKey.UPDATED_DATE, ProjectUtil.getFormattedDate());
  }

  private void updateBatchMap(Map<String, Object> map) {
    String courseId = (String) map.get(JsonKey.COURSE_ID);
    String enrollmentType = (String) map.get(JsonKey.ENROLLMENT_TYPE);
    if (JsonKey.OPEN.equals(enrollmentType)) {
      if (openBatchMap.containsKey(courseId)) {
        openBatchMap.get(courseId).add(map);
      } else {
        List<Map<String, Object>> batchList = new ArrayList<>();
        batchList.add(map);
        openBatchMap.put(courseId, batchList);
      }
    } else {
      if (privateBatchMap.containsKey(courseId)) {
        privateBatchMap.get(courseId).add(map);
      } else {
        List<Map<String, Object>> batchList = new ArrayList<>();
        batchList.add(map);
        privateBatchMap.put(courseId, batchList);
      }
    }
  }

  private void contentMapUpdate(boolean increment) {
    for (Map.Entry<String, List<Map<String, Object>>> openBatchList : openBatchMap.entrySet()) {
      String contentName = CourseBatchSchedulerUtil.getContentName(JsonKey.OPEN);
      updateContentMapCount(
          increment, contentName, openBatchList.getKey(), openBatchList.getValue().size());
    }
    for (Map.Entry<String, List<Map<String, Object>>> privateBatchList :
        privateBatchMap.entrySet()) {
      String contentName = CourseBatchSchedulerUtil.getContentName(JsonKey.INVITE_ONLY);
      updateContentMapCount(
          increment, contentName, privateBatchList.getKey(), privateBatchList.getValue().size());
    }
    updateEkstepAndDb();
  }

  private void updateContentMapCount(
      boolean increment, String contentName, String courseId, int size) {
    int val = (int) contentMap.get(courseId).getOrDefault(contentName, 0);
    if (increment) {
      val += size;
    } else {
      val -= size;
      if (val < 0) {
        val = 0;
      }
    }
    contentMap.get(courseId).put(contentName, val);
  }

  private void getContentAndStore(Map<String, List<Map<String, Object>>> batchMap) {
    Map<String, String> ekstepHeader = CourseBatchSchedulerUtil.headerMap;
    for (Map.Entry<String, List<Map<String, Object>>> batchList : batchMap.entrySet()) {
      String courseId = batchList.getKey();
      if (!contentMap.containsKey(courseId)) {
        Map<String, Object> ekStepContent =
            CourseEnrollmentActor.getCourseObjectFromEkStep(courseId, ekstepHeader);
        if (null != ekStepContent && ekStepContent.size() > 0) {
          contentMap.put(courseId, ekStepContent);
        }
      }
    }
  }

  private void updateEkstepAndDb() {
    boolean response;
    for (Map.Entry<String, Map<String, Object>> ekStepUpdateMap : contentMap.entrySet()) {
      String courseId = ekStepUpdateMap.getKey();
      String contentName = CourseBatchSchedulerUtil.getContentName(JsonKey.OPEN);
      response =
          CourseBatchSchedulerUtil.updateEkstepContent(
              courseId, contentName, (int) ekStepUpdateMap.getValue().get(contentName));
      if (response) {
        openBatchMap
            .get(courseId)
            .forEach(
                map -> {
                  if (CourseBatchSchedulerUtil.updateDataIntoES(map)) {
                    CourseBatchSchedulerUtil.updateDataIntoCassandra(map);
                  }
                });
        contentName = CourseBatchSchedulerUtil.getContentName(JsonKey.INVITE_ONLY);
        response =
            CourseBatchSchedulerUtil.updateEkstepContent(
                courseId, contentName, (int) ekStepUpdateMap.getValue().get(contentName));
        if (response) {
          openBatchMap
              .get(courseId)
              .forEach(
                  map -> {
                    if (CourseBatchSchedulerUtil.updateDataIntoES(map)) {
                      CourseBatchSchedulerUtil.updateDataIntoCassandra(map);
                    }
                  });
        }
      }
    }
  }
}
