package org.sunbird.learner.actors;

import static akka.testkit.JavaTestKit.duration;
import static org.junit.Assert.assertEquals;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchUtil;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.ProjectUtil.EsIndex;
import org.sunbird.common.models.util.ProjectUtil.EsType;
import org.sunbird.common.models.util.ProjectUtil.OrgStatus;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.Application;
import org.sunbird.learner.util.Util;

/**
 * @author arvind.
 */
//@Ignore
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OrganisationManagementActorTest {

    static ActorSystem system;
    static CassandraOperation operation = ServiceFactory.getInstance();
    final static Props props = Props.create(OrganisationManagementActor.class);
    final static Props propsUser = Props.create(UserManagementActor.class);
    static Util.DbInfo orgTypeDbInfo = null;
    static Util.DbInfo userManagementDB = null;
    static Util.DbInfo addressDB = null;
    static Util.DbInfo orgDB = null;
    static Util.DbInfo locationDB = null;
    private static String orgTypeId1 = "";
    private static String orgTypeId2 = "";
    public static String orgId = "";
    public static String addressId = "";
    public static String usrId = "123";//TODO:change while committing
    public static String OrgIDWithoutSourceAndExternalId = "";
    public static String OrgIdWithSourceAndExternalId = "";
    public final static String source = "Test";
    public final static String externalId = "test123";
    public static final String HASH_TAG_ID = "hashTag011";
    public static final String LOCATION_ID = "icu9289w";
    public static final String EXTERNAL_ID = "ex00001lvervk";
    public static final String PROVIDER = "pr00001kfej";
    public static final String CHANNEL = "hjryr9349";
    public static final String parentOrgId = "778euffnvrj";
  private static final String USER_ID = "vcurc633r89";
  private static Util.DbInfo userDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
  private static Util.DbInfo userOrgDbInfo = Util.dbInfoMap.get(JsonKey.USER_ORG_DB);
    
    @BeforeClass
    public static void setUp() {
      CassandraOperation operation = ServiceFactory.getInstance();
        Application.startLocalActorSystem();
        system = ActorSystem.create("system");
        Util.checkCassandraDbConnections(JsonKey.SUNBIRD);
        userManagementDB = Util.dbInfoMap.get(JsonKey.USER_DB);
        addressDB = Util.dbInfoMap.get(JsonKey.ADDRESS_DB);
        orgTypeDbInfo = Util.dbInfoMap.get(JsonKey.ORG_TYPE_DB);
        orgDB = Util.dbInfoMap.get(JsonKey.ORG_DB);
        locationDB = Util.dbInfoMap.get(JsonKey.GEO_LOCATION_DB);
        Map<String , Object> geoLocation = new HashMap<>();
        // need to delete in after class...
        geoLocation.put(JsonKey.ID , LOCATION_ID);
        //geoLocation.put(JsonKey.LOCATION_ID , LOCATION_ID);
        operation.insertRecord(locationDB.getKeySpace(), locationDB.getTableName() ,geoLocation);
        Map<String , Object> parentOrg = new HashMap<>();
        parentOrg.put(JsonKey.ID , parentOrgId);
        operation.upsertRecord(orgDB.getKeySpace() , orgDB.getTableName() , parentOrg);

      Map<String , Object> userMap = new HashMap<>();
      userMap.put(JsonKey.ID , USER_ID);
      //userMap.put(JsonKey.ROOT_ORG_ID, ROOT_ORG_ID);
      operation.insertRecord(userDbInfo.getKeySpace(), userDbInfo.getTableName(), userMap);
      userMap.put(JsonKey.USER_ID, USER_ID);
      ElasticSearchUtil.createData(EsIndex.sunbird.getIndexName(), EsType.user.getTypeName(), USER_ID, userMap);

      Map<String , Object> userMap1 = new HashMap<>();
      userMap1.put(JsonKey.ID , USER_ID+"01");
      //userMap.put(JsonKey.ROOT_ORG_ID, ROOT_ORG_ID);
      operation.insertRecord(userDbInfo.getKeySpace(), userDbInfo.getTableName(), userMap1);
      userMap1.put(JsonKey.USER_ID, USER_ID+"01");
      ElasticSearchUtil.createData(EsIndex.sunbird.getIndexName(), EsType.user.getTypeName(), USER_ID, userMap1);


    }
    //@Test
    public void test10createUserForId() {
      TestKit probe = new TestKit(system);
      ActorRef subject = system.actorOf(propsUser);

      Request reqObj = new Request();
      reqObj.setRequestId("1");
      reqObj.setOperation(ActorOperations.CREATE_USER.getValue());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.USERNAME, "test04buser");
      innerMap.put(JsonKey.EMAIL, "test04buser@xyzab.com");
      innerMap.put(JsonKey.PASSWORD , "password");
      Map<String , Object> request = new HashMap<String , Object>();
      request.put(JsonKey.USER , innerMap);
      reqObj.setRequest(request);

      subject.tell(reqObj, probe.getRef());
      Response res =  probe.expectMsgClass(Response.class);
      usrId = (String)res.get(JsonKey.USER_ID);
    }

  @Test
  public void test11createOrgForId(){
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.CREATE_ORG.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    Map<String , Object> orgMap = new HashMap<String , Object>();
    orgMap.put(JsonKey.ORGANISATION_NAME , "CBSE");
    orgMap.put(JsonKey.DESCRIPTION, "Central Board of Secondary Education");
    orgMap.put(JsonKey.ORG_CODE, "CBSE");
    orgMap.put(JsonKey.HASHTAGID , HASH_TAG_ID);
    orgMap.put(JsonKey.PARENT_ORG_ID , parentOrgId);
    List<Map<String , Object>> contactDetails = new ArrayList<>();
    Map<String , Object> contactDetail = new HashMap<>();
    contactDetail.put("fax","100");
    contactDetails.add(contactDetail);
    orgMap.put(JsonKey.CONTACT_DETAILS , contactDetails);
    orgMap.put(JsonKey.LOC_ID , LOCATION_ID);
    //orgMap.put(JsonKey.CHANNEL, "test");
    Map<String,Object> address = new HashMap<String,Object>();
    address.put(JsonKey.CITY, "Hyderabad");
    address.put("state", "Andra Pradesh");
    address.put("country", "India");
    address.put("zipCode", "466899");
    innerMap.put(JsonKey.ADDRESS, address);
    innerMap.put(JsonKey.ORGANISATION , orgMap);
    reqObj.getRequest().put(JsonKey.REQUESTED_BY,"123234345");
    reqObj.setRequest(innerMap);
    subject.tell(reqObj, probe.getRef());
    Response resp = probe.expectMsgClass(duration("200 second"),Response.class);
    orgId = (String) resp.getResult().get(JsonKey.ORGANISATION_ID);
    System.out.println("orgId : "+orgId);
    try {
      Thread.sleep(20000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }



    @Test
    public void test11createOrgForIdWithDuplicateHashTagId(){
      TestKit probe = new TestKit(system);
      ActorRef subject = system.actorOf(props);

      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.CREATE_ORG.getValue());
      HashMap<String, Object> innerMap = new HashMap<>();
      Map<String , Object> orgMap = new HashMap<String , Object>();
      orgMap.put(JsonKey.ORGANISATION_NAME , "CBSE");
      orgMap.put(JsonKey.DESCRIPTION, "Central Board of Secondary Education");
      orgMap.put(JsonKey.ORG_CODE, "CBSE");
      //orgMap.put(JsonKey.PROVIDER, PROVIDER);
      //orgMap.put(JsonKey.EXTERNAL_ID, EXTERNAL_ID);
      orgMap.put(JsonKey.HASHTAGID , HASH_TAG_ID);
      orgMap.put(JsonKey.PARENT_ORG_ID , parentOrgId);
      List<Map<String , Object>> contactDetails = new ArrayList<>();
      Map<String , Object> contactDetail = new HashMap<>();
      contactDetail.put("fax","100");
      contactDetails.add(contactDetail);
      orgMap.put(JsonKey.CONTACT_DETAILS , contactDetails);
      orgMap.put(JsonKey.LOC_ID , LOCATION_ID);
      //orgMap.put(JsonKey.CHANNEL, "test");
      Map<String,Object> address = new HashMap<String,Object>();
      address.put(JsonKey.CITY, "Hyderabad");
      address.put("state", "Andra Pradesh");
      address.put("country", "India");
      address.put("zipCode", "466899");
      innerMap.put(JsonKey.ADDRESS, address);
      innerMap.put(JsonKey.ORGANISATION , orgMap);

      reqObj.setRequest(innerMap);
      subject.tell(reqObj, probe.getRef());
      ProjectCommonException resp = probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
      /*orgId = (String) resp.getResult().get(JsonKey.ORGANISATION_ID);
      System.out.println("orgId with dpl hashtag id : "+orgId);*/
      try {
        Thread.sleep(4000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

  @Test
  public void test11joinUserOrganisation(){
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.JOIN_USER_ORGANISATION.getValue());

    HashMap<String, Object> innerMap = new HashMap<>();
    reqObj.getRequest().put(JsonKey.USER_ORG , innerMap);

    innerMap.put(JsonKey.ORGANISATION_ID , orgId);
    innerMap.put(JsonKey.USER_ID , USER_ID);

    List<String> roles = new ArrayList<>();
    roles.add("ADMIN");
    innerMap.put(JsonKey.ROLES, roles);

    subject.tell(reqObj, probe.getRef());
    Response resp = probe.expectMsgClass(duration("200 second"),Response.class);
  }

  @Test
  public void test11joinUserOrganisationInvalidOrgId(){
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.JOIN_USER_ORGANISATION.getValue());

    HashMap<String, Object> innerMap = new HashMap<>();
    reqObj.getRequest().put(JsonKey.USER_ORG , innerMap);

    innerMap.put(JsonKey.ORGANISATION_ID , orgId+"bjic3r9");
    innerMap.put(JsonKey.USER_ID , USER_ID);

    List<String> roles = new ArrayList<>();
    roles.add("ADMIN");
    innerMap.put(JsonKey.ROLES, roles);

    subject.tell(reqObj, probe.getRef());
    ProjectCommonException resp = probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
  }

  @Test
  public void test11joinUserOrganisationInvalidOrgIdNull(){
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.JOIN_USER_ORGANISATION.getValue());

    HashMap<String, Object> innerMap = new HashMap<>();
    reqObj.getRequest().put(JsonKey.USER_ORG , innerMap);

    innerMap.put(JsonKey.ORGANISATION_ID , null);
    innerMap.put(JsonKey.USER_ID , USER_ID);

    List<String> roles = new ArrayList<>();
    roles.add("ADMIN");
    innerMap.put(JsonKey.ROLES, roles);

    subject.tell(reqObj, probe.getRef());
    ProjectCommonException resp = probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
  }

  @Test
  public void test11joinUserOrganisationInvalidRequestData(){
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.JOIN_USER_ORGANISATION.getValue());

    reqObj.getRequest().put(JsonKey.USER_ORG , null);


    subject.tell(reqObj, probe.getRef());
    ProjectCommonException resp = probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
  }

  @Test
  public void test11joinUserOrganisationApproveUserOrg(){
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.APPROVE_USER_ORGANISATION.getValue());

    HashMap<String, Object> innerMap = new HashMap<>();
    reqObj.getRequest().put(JsonKey.USER_ORG , innerMap);

    innerMap.put(JsonKey.ORGANISATION_ID , orgId);
    innerMap.put(JsonKey.USER_ID , USER_ID);

    List<String> roles = new ArrayList<>();
    roles.add("ADMIN");
    innerMap.put(JsonKey.ROLES, roles);

    subject.tell(reqObj, probe.getRef());
    Response resp = probe.expectMsgClass(duration("200 second"),Response.class);
  }

  @Test
  public void test11joinUserOrganisationApproveUserOrgInvalidRequestData(){
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.APPROVE_USER_ORGANISATION.getValue());

    HashMap<String, Object> innerMap = new HashMap<>();
    reqObj.getRequest().put(JsonKey.USER_ORG , null);

    innerMap.put(JsonKey.ORGANISATION_ID , orgId);
    innerMap.put(JsonKey.USER_ID , USER_ID);

    List<String> roles = new ArrayList<>();
    roles.add("ADMIN");
    innerMap.put(JsonKey.ROLES, roles);

    subject.tell(reqObj, probe.getRef());
    ProjectCommonException resp = probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
  }

  @Test
  public void test11joinUserOrganisationApproveUserOrgInvalidRequestDataWrongOrg(){
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.APPROVE_USER_ORGANISATION.getValue());

    HashMap<String, Object> innerMap = new HashMap<>();
    reqObj.getRequest().put(JsonKey.USER_ORG , innerMap);

    innerMap.put(JsonKey.ORGANISATION_ID , orgId+"bj32u");
    innerMap.put(JsonKey.USER_ID , USER_ID);

    List<String> roles = new ArrayList<>();
    roles.add("ADMIN");
    innerMap.put(JsonKey.ROLES, roles);

    subject.tell(reqObj, probe.getRef());
    ProjectCommonException resp = probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
  }

  @Test
  public void test11joinUserOrganisationApproveUserOrgInvalidRequestNullUser(){
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.APPROVE_USER_ORGANISATION.getValue());

    HashMap<String, Object> innerMap = new HashMap<>();
    reqObj.getRequest().put(JsonKey.USER_ORG , innerMap);

    innerMap.put(JsonKey.ORGANISATION_ID , orgId);
    innerMap.put(JsonKey.USER_ID , null);

    List<String> roles = new ArrayList<>();
    roles.add("ADMIN");
    innerMap.put(JsonKey.ROLES, roles);

    subject.tell(reqObj, probe.getRef());
    ProjectCommonException resp = probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
  }

  @Test
  public void test11joinUserOrganisationRejectUserOrg(){
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.REJECT_USER_ORGANISATION.getValue());

    HashMap<String, Object> innerMap = new HashMap<>();
    reqObj.getRequest().put(JsonKey.USER_ORG , innerMap);

    innerMap.put(JsonKey.ORGANISATION_ID , orgId);
    innerMap.put(JsonKey.USER_ID , USER_ID);

    subject.tell(reqObj, probe.getRef());
    Response resp = probe.expectMsgClass(duration("200 second"),Response.class);
  }

  @Test
  public void test11joinUserOrganisationRejectUserOrgInvalidOrgData(){
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.REJECT_USER_ORGANISATION.getValue());

    HashMap<String, Object> innerMap = new HashMap<>();
    reqObj.getRequest().put(JsonKey.USER_ORG , innerMap);

    innerMap.put(JsonKey.ORGANISATION_ID , orgId+"eig");
    innerMap.put(JsonKey.USER_ID , USER_ID);

    subject.tell(reqObj, probe.getRef());
    ProjectCommonException resp = probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
  }

  @Test
  public void test11joinUserOrganisationRejectUserOrgInvalidOrgDataNullOrg(){
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.REJECT_USER_ORGANISATION.getValue());

    HashMap<String, Object> innerMap = new HashMap<>();
    reqObj.getRequest().put(JsonKey.USER_ORG ,innerMap );

    innerMap.put(JsonKey.ORGANISATION_ID , null);
    innerMap.put(JsonKey.USER_ID , USER_ID);

    subject.tell(reqObj, probe.getRef());
    ProjectCommonException resp = probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
  }

  @Test
  public void test11joinUserOrganisationRejectUserOrgInvalidOrgDataNull(){
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.REJECT_USER_ORGANISATION.getValue());

    reqObj.getRequest().put(JsonKey.USER_ORG ,null );

    subject.tell(reqObj, probe.getRef());
    ProjectCommonException resp = probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
  }

    @Test
    public void testInvalidOperation(){
        TestKit probe = new TestKit(system);
        ActorRef subject = system.actorOf(props);

        Request reqObj = new Request();
        reqObj.setOperation("INVALID_OPERATION");

        subject.tell(reqObj, probe.getRef());
        probe.expectMsgClass(ProjectCommonException.class);
    }

    @Test
    public void testInvalidMessageType(){
        TestKit probe = new TestKit(system);
        ActorRef subject = system.actorOf(props);

        subject.tell("Invalid Type", probe.getRef());
        probe.expectMsgClass(ProjectCommonException.class);
    }
    
    @Test
    public void test12testCreateOrgWithoutSourceAndExternalIdSuc(){
      TestKit probe = new TestKit(system);
      ActorRef subject = system.actorOf(props);

      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.CREATE_ORG.getValue());
      HashMap<String, Object> innerMap = new HashMap<>();
      Map<String , Object> orgMap = new HashMap<String , Object>();
      orgMap.put(JsonKey.ORGANISATION_NAME , "CBSE");
      orgMap.put(JsonKey.DESCRIPTION, "Central Board of Secondary Education");
      orgMap.put("orgCode", "CBSE");
      orgMap.put("isRootOrg", true);
      orgMap.put("channel", CHANNEL);
      innerMap.put(JsonKey.ORGANISATION , orgMap);

      reqObj.setRequest(innerMap);
      subject.tell(reqObj, probe.getRef());
      Response resp = probe.expectMsgClass(duration("200 second"),Response.class);
      OrgIDWithoutSourceAndExternalId = (String) resp.getResult().get(JsonKey.ORGANISATION_ID);
      System.out.println("OrgIDWithoutSourceAndExternalId : "+OrgIDWithoutSourceAndExternalId);
      Assert.assertNotNull(OrgIDWithoutSourceAndExternalId);
      try {
        Thread.sleep(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

  @Test
  public void test12testCreateOrgWithoutSourceAndExternalIdSucDuplicateChannel(){
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.CREATE_ORG.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    Map<String , Object> orgMap = new HashMap<String , Object>();
    orgMap.put(JsonKey.ORGANISATION_NAME , "CBSE");
    orgMap.put(JsonKey.DESCRIPTION, "Central Board of Secondary Education");
    orgMap.put("orgCode", "CBSE");
    orgMap.put("isRootOrg", true);
    orgMap.put("channel", CHANNEL);
    innerMap.put(JsonKey.ORGANISATION , orgMap);

    reqObj.setRequest(innerMap);
    subject.tell(reqObj, probe.getRef());
    ProjectCommonException resp = probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
    try {
      Thread.sleep(4000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
    
    @Test
    public void test13CreateOrgWithSourceAndExternalIdSuc(){
      
      try {
        Thread.sleep(4000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      
      TestKit probe = new TestKit(system);
      ActorRef subject = system.actorOf(props);

      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.CREATE_ORG.getValue());
      HashMap<String, Object> innerMap = new HashMap<>();
      Map<String , Object> orgMap = new HashMap<String , Object>();
      orgMap.put(JsonKey.ORGANISATION_NAME , "CBSE");
      orgMap.put(JsonKey.DESCRIPTION, "Central Board of Secondary Education");
      orgMap.put("orgCode", "CBSE");
      orgMap.put(JsonKey.PROVIDER, PROVIDER);
      orgMap.put(JsonKey.EXTERNAL_ID, EXTERNAL_ID);
     // orgMap.put("channel", "test1");
      innerMap.put(JsonKey.ORGANISATION , orgMap);

      reqObj.setRequest(innerMap);
      subject.tell(reqObj, probe.getRef());
      Response resp = probe.expectMsgClass(duration("200 second"),Response.class);
      OrgIdWithSourceAndExternalId = (String) resp.getResult().get(JsonKey.ORGANISATION_ID);
      System.out.println("OrgIdWithSourceAndExternalId : "+OrgIdWithSourceAndExternalId);
      Assert.assertNotNull(OrgIdWithSourceAndExternalId);
    }

  @Test
  public void test13CreateOrgWithSourceAndExternalIdSucDuplicate(){

    try {
      Thread.sleep(4000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.CREATE_ORG.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    Map<String , Object> orgMap = new HashMap<String , Object>();
    orgMap.put(JsonKey.ORGANISATION_NAME , "CBSE");
    orgMap.put(JsonKey.DESCRIPTION, "Central Board of Secondary Education");
    orgMap.put("orgCode", "CBSE");
    orgMap.put(JsonKey.PROVIDER, PROVIDER);
    orgMap.put(JsonKey.EXTERNAL_ID, EXTERNAL_ID);
    // orgMap.put("channel", "test1");
    innerMap.put(JsonKey.ORGANISATION , orgMap);

    reqObj.setRequest(innerMap);
    subject.tell(reqObj, probe.getRef());
    ProjectCommonException resp = probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
  }
    
    @Test
    public void test14CreateOrgWithSameSourceAndExternalIdExc(){
      try {
        Thread.sleep(4000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      TestKit probe = new TestKit(system);
      ActorRef subject = system.actorOf(props);

      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.CREATE_ORG.getValue());
      HashMap<String, Object> innerMap = new HashMap<>();
      Map<String , Object> orgMap = new HashMap<String , Object>();
      orgMap.put(JsonKey.ORGANISATION_NAME , "CBSE");
      orgMap.put(JsonKey.DESCRIPTION, "Central Board of Secondary Education");
      orgMap.put("orgCode", "CBSE");
      orgMap.put(JsonKey.PROVIDER, PROVIDER);
      orgMap.put(JsonKey.EXTERNAL_ID, EXTERNAL_ID);
      //orgMap.put("channel", CHANNEL);
      innerMap.put(JsonKey.ORGANISATION , orgMap);

      reqObj.setRequest(innerMap);
      subject.tell(reqObj, probe.getRef());
      probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
    }
    
    @Test
    public void test15CreateOrgWithBlankSourceAndExternalIdExc(){
      TestKit probe = new TestKit(system);
      ActorRef subject = system.actorOf(props);

      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.CREATE_ORG.getValue());
      HashMap<String, Object> innerMap = new HashMap<>();
      Map<String , Object> orgMap = new HashMap<String , Object>();
      orgMap.put(JsonKey.ORGANISATION_NAME , "CBSE");
      orgMap.put(JsonKey.DESCRIPTION, "Central Board of Secondary Education");
      orgMap.put("orgCode", "CBSE");
      orgMap.put(JsonKey.PROVIDER, null);
      orgMap.put("externalId", null);
      //orgMap.put("channel", CHANNEL);
      innerMap.put(JsonKey.ORGANISATION , orgMap);

      reqObj.setRequest(innerMap);
      subject.tell(reqObj, probe.getRef());
      probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
    }
    
    
    //@Test
    public void test16CreateOrgRootWithoutChannelExc(){
      TestKit probe = new TestKit(system);
      ActorRef subject = system.actorOf(props);

      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.CREATE_ORG.getValue());
      HashMap<String, Object> innerMap = new HashMap<>();
      Map<String , Object> orgMap = new HashMap<String , Object>();
      orgMap.put(JsonKey.ORGANISATION_NAME , "AP Board");
      orgMap.put(JsonKey.DESCRIPTION, "AndhraPradesh Board");
      orgMap.put(JsonKey.ORG_TYPE, "Training");
      orgMap.put(JsonKey.CHANNEL, null);
      orgMap.put("preferredLanguage", "English");
      orgMap.put("homeUrl", "https:testUrl");
      orgMap.put(JsonKey.ORG_CODE, "AP");
      orgMap.put(JsonKey.IS_ROOT_ORG, true);
      innerMap.put(JsonKey.ORGANISATION , orgMap);

      reqObj.setRequest(innerMap);
      subject.tell(reqObj, probe.getRef());
      probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
    }
    
    @Test
    public void test17CreateOrgInvalidParentIdExc(){
      TestKit probe = new TestKit(system);
      ActorRef subject = system.actorOf(props);

      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.CREATE_ORG.getValue());
      HashMap<String, Object> innerMap = new HashMap<>();
      Map<String , Object> orgMap = new HashMap<String , Object>();
      orgMap.put(JsonKey.ORGANISATION_NAME , "Tamil Nadu ");
      orgMap.put(JsonKey.DESCRIPTION, "Tamil Nadu Board");
      orgMap.put(JsonKey.PARENT_ORG_ID, "CBSE");
      orgMap.put(JsonKey.ORG_TYPE, "Training");
      orgMap.put("imgUrl", "https://testimgUrl");
      orgMap.put(JsonKey.CHANNEL, "Ekstep");
      orgMap.put("preferredLanguage", "Tamil");
      orgMap.put("homeUrl", "https:testUrl");
      orgMap.put(JsonKey.ORG_CODE, "TN");
      orgMap.put(JsonKey.IS_ROOT_ORG, false);
      innerMap.put(JsonKey.ORGANISATION , orgMap);

      reqObj.setRequest(innerMap);
      subject.tell(reqObj, probe.getRef());
      probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
    }
    
    @Test
    public void test18ApproveOrgSuc(){
      TestKit probe = new TestKit(system);
      
      ActorRef subject = system.actorOf(props);

      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.APPROVE_ORG.getValue());
      HashMap<String, Object> innerMap = new HashMap<>();
      Map<String , Object> orgMap = new HashMap<String , Object>();
      orgMap.put(JsonKey.ORGANISATION_ID , orgId);
      innerMap.put(JsonKey.ORGANISATION , orgMap);
      innerMap.put(JsonKey.REQUESTED_BY , "ALPHA");
      reqObj.setRequest(innerMap);
      subject.tell(reqObj, probe.getRef());
      probe.expectMsgClass(duration("200 second"),Response.class);
    }

    
    @Test
    public void test19ApproveOrgExc(){
      TestKit probe = new TestKit(system);
      
      ActorRef subject = system.actorOf(props);

      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.APPROVE_ORG.getValue());
      HashMap<String, Object> innerMap = new HashMap<>();
      Map<String , Object> orgMap = new HashMap<String , Object>();
      orgMap.put(JsonKey.ORGANISATION_ID , null);
      innerMap.put(JsonKey.ORGANISATION , orgMap);
      reqObj.setRequest(innerMap);
      subject.tell(reqObj, probe.getRef());
      probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
    }

  @Test
  public void test19ApproveOrgExcInvalidOrgId(){
    TestKit probe = new TestKit(system);

    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.APPROVE_ORG.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    Map<String , Object> orgMap = new HashMap<String , Object>();
    orgMap.put(JsonKey.ORGANISATION_ID , orgId+"6389");
    innerMap.put(JsonKey.ORGANISATION , orgMap);
    reqObj.setRequest(innerMap);
    subject.tell(reqObj, probe.getRef());
    probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
  }
    
    @Test
    public void test20UpdateStatusSuc(){
      TestKit probe = new TestKit(system);
      
      ActorRef subject = system.actorOf(props);

      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.UPDATE_ORG_STATUS.getValue());
      HashMap<String, Object> innerMap = new HashMap<>();
      Map<String , Object> orgMap = new HashMap<String , Object>();
      orgMap.put(JsonKey.ORGANISATION_ID , orgId);
      orgMap.put(JsonKey.STATUS, new BigInteger(String.valueOf(OrgStatus.RETIRED.getValue())));
      innerMap.put(JsonKey.ORGANISATION , orgMap);
      reqObj.setRequest(innerMap);
      subject.tell(reqObj, probe.getRef());
      probe.expectMsgClass(duration("200 second"),Response.class);
    }

  @Test
  public void test20UpdateStatusSucInvalidOrgId(){
    TestKit probe = new TestKit(system);

    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.UPDATE_ORG_STATUS.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    Map<String , Object> orgMap = new HashMap<String , Object>();
    orgMap.put(JsonKey.ORGANISATION_ID , orgId+"ucic");
    orgMap.put(JsonKey.STATUS, new BigInteger(String.valueOf(OrgStatus.RETIRED.getValue())));
    innerMap.put(JsonKey.ORGANISATION , orgMap);
    reqObj.setRequest(innerMap);
    subject.tell(reqObj, probe.getRef());
    probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
  }

  @Test
  public void test20UpdateStatusSucInvalidStateTransition(){
    TestKit probe = new TestKit(system);

    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.UPDATE_ORG_STATUS.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    Map<String , Object> orgMap = new HashMap<String , Object>();
    orgMap.put(JsonKey.ORGANISATION_ID , orgId);
    orgMap.put(JsonKey.STATUS, new BigInteger(String.valueOf(OrgStatus.RETIRED.getValue())));
    orgMap.put(JsonKey.STATUS, new BigInteger("10"));
    innerMap.put(JsonKey.ORGANISATION , orgMap);
    reqObj.setRequest(innerMap);
    subject.tell(reqObj, probe.getRef());
    probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
  }
    
    @Test
    public void test21UpdateStatusEx(){
      TestKit probe = new TestKit(system);
      
      ActorRef subject = system.actorOf(props);

      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.UPDATE_ORG_STATUS.getValue());
      HashMap<String, Object> innerMap = new HashMap<>();
      Map<String , Object> orgMap = new HashMap<String , Object>();
      orgMap.put(JsonKey.PROVIDER , source);
      orgMap.put(JsonKey.EXTERNAL_ID, externalId);
      orgMap.put(JsonKey.STATUS, new BigInteger("10"));
      innerMap.put(JsonKey.ORGANISATION , orgMap);
      reqObj.setRequest(innerMap);
      subject.tell(reqObj, probe.getRef());
      probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
    }
    
    @Test
    public void test22UpdateOrgExc(){
      TestKit probe = new TestKit(system);
      
      ActorRef subject = system.actorOf(props);

      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.UPDATE_ORG.getValue());
      HashMap<String, Object> innerMap = new HashMap<>();
      Map<String , Object> orgMap = new HashMap<String , Object>();
      orgMap.put(JsonKey.ORGANISATION_ID , "");
      orgMap.put("imgUrl", "test");
      innerMap.put(JsonKey.ORGANISATION , orgMap);
      reqObj.setRequest(innerMap);
      subject.tell(reqObj, probe.getRef());
      probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
    }

  @Test
  public void test22UpdateOrgExcInvalidOrgId(){
    TestKit probe = new TestKit(system);

    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.UPDATE_ORG.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    Map<String , Object> orgMap = new HashMap<String , Object>();
    orgMap.put(JsonKey.ORGANISATION_ID , orgId+"bdu438f");
    orgMap.put("imgUrl", "test");
    innerMap.put(JsonKey.ORGANISATION , orgMap);
    reqObj.setRequest(innerMap);
    subject.tell(reqObj, probe.getRef());
    probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
  }
    
    @Test
    public void test23UpdateOrgSuc(){
      TestKit probe = new TestKit(system);
      
      ActorRef subject = system.actorOf(props);

      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.UPDATE_ORG.getValue());
      HashMap<String, Object> innerMap = new HashMap<>();
      Map<String , Object> orgMap = new HashMap<String , Object>();
      orgMap.put(JsonKey.ORGANISATION_ID , orgId);
      orgMap.put("imgUrl", "test");
      //orgMap.put(JsonKey.ORG_TYPE, "ORG_TYPE_0002");
      innerMap.put(JsonKey.ORGANISATION , orgMap);
      reqObj.setRequest(innerMap);
      subject.tell(reqObj, probe.getRef());
      probe.expectMsgClass(duration("200 second"),Response.class);
    }
    
    @Test
    public void test23UpdateOrgExc(){
      TestKit probe = new TestKit(system);
      
      ActorRef subject = system.actorOf(props);

      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.UPDATE_ORG.getValue());
      HashMap<String, Object> innerMap = new HashMap<>();
      Map<String , Object> orgMap = new HashMap<String , Object>();
      orgMap.put(JsonKey.ORGANISATION_ID , orgId);
      orgMap.put("imgUrl", "test");
      orgMap.put(JsonKey.PROVIDER, PROVIDER);
      orgMap.put(JsonKey.EXTERNAL_ID, EXTERNAL_ID);
      innerMap.put(JsonKey.ORGANISATION , orgMap);
      reqObj.setRequest(innerMap);
      subject.tell(reqObj, probe.getRef());
      probe.expectMsgClass(ProjectCommonException.class);
    }

  @Test
  public void test23UpdateOrgSuc001(){
    TestKit probe = new TestKit(system);

    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.UPDATE_ORG.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    Map<String , Object> orgMap = new HashMap<String , Object>();
    orgMap.put(JsonKey.EXTERNAL_ID , EXTERNAL_ID);
    orgMap.put(JsonKey.PROVIDER , PROVIDER);
    orgMap.put(JsonKey.LOC_ID , LOCATION_ID);
    orgMap.put(JsonKey.PARENT_ORG_ID, parentOrgId);
    orgMap.put("imgUrl", "test");
    List<Map<String , Object>> contactDetails = new ArrayList<>();
    Map<String , Object> contactDetail = new HashMap<>();
    contactDetail.put("fax","100");
    contactDetails.add(contactDetail);
    orgMap.put(JsonKey.CONTACT_DETAILS , contactDetails);
    /*Map<String ,Object> address = new HashMap<>();
    address.put(JsonKey.CITY , "STATE");
    //orgMap.put(JsonKey.ADDRESS , address);*/
    innerMap.put(JsonKey.ORGANISATION , orgMap);
    reqObj.setRequest(innerMap);
    subject.tell(reqObj, probe.getRef());
    probe.expectMsgClass(duration("200 second"),Response.class);
  }
  
  @Test
  public void test23UpdateOrgSuc002(){
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.UPDATE_ORG.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    Map<String , Object> orgMap = new HashMap<String , Object>();
    orgMap.put(JsonKey.ORGANISATION_ID , OrgIdWithSourceAndExternalId);
    orgMap.put(JsonKey.HASHTAGID , HASH_TAG_ID);
    orgMap.put("imgUrl", "test");
    innerMap.put(JsonKey.ORGANISATION , orgMap);
    reqObj.setRequest(innerMap);
    subject.tell(reqObj, probe.getRef());
    probe.expectMsgClass(ProjectCommonException.class);
  }
  
  @Test
  public void testCreateOrg002(){
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.CREATE_ORG.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    Map<String , Object> orgMap = new HashMap<String , Object>();
    orgMap.put(JsonKey.ORGANISATION_NAME , "CBSE");
    orgMap.put(JsonKey.DESCRIPTION, "Central Board of Secondary Education");
    orgMap.put(JsonKey.LOC_ID, "test");
    innerMap.put(JsonKey.ORGANISATION , orgMap);
    reqObj.setRequest(innerMap);
    subject.tell(reqObj, probe.getRef());
    probe.expectMsgClass(ProjectCommonException.class);
  }
  
  @Test
  public void testCreateOrg003(){
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.CREATE_ORG.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    Map<String , Object> orgMap = new HashMap<String , Object>();
    orgMap.put(JsonKey.ORGANISATION_NAME , "CBSE");
    orgMap.put(JsonKey.DESCRIPTION, "Central Board of Secondary Education");
    orgMap.put(JsonKey.ORG_TYPE, "skmdlfk");
    innerMap.put(JsonKey.ORGANISATION , orgMap);
    reqObj.setRequest(innerMap);
    subject.tell(reqObj, probe.getRef());
    probe.expectMsgClass(ProjectCommonException.class);
  }
    
    @SuppressWarnings("unchecked")
    @Test
    public void test24GetOrgSuc() {
        TestKit probe = new TestKit(system);
        ActorRef subject = system.actorOf(props);

        Request reqObj = new Request();
        reqObj.setOperation(ActorOperations.GET_ORG_DETAILS.getValue());
        HashMap<String, Object> innerMap = new HashMap<>();
        Map<String, Object> orgMap = new HashMap<String, Object>();
        orgMap.put(JsonKey.ORGANISATION_ID, orgId);
        innerMap.put(JsonKey.ORGANISATION, orgMap);

        reqObj.setRequest(innerMap);
        subject.tell(reqObj, probe.getRef());
        Response resp =probe.expectMsgClass(duration("200 second"),Response.class);
        try{
        addressId = (String) (((Map<String,Object>)resp.getResult().get(JsonKey.RESPONSE)).get(JsonKey.ADDRESS_ID));
        }catch(Exception ex){
          ex.printStackTrace();
        }
    }
    
    @Test
    public void test25GetOrgExc() {
        TestKit probe = new TestKit(system);
        ActorRef subject = system.actorOf(props);

        Request reqObj = new Request();
        reqObj.setOperation(ActorOperations.GET_ORG_DETAILS.getValue());
        HashMap<String, Object> innerMap = new HashMap<>();
        Map<String, Object> orgMap = new HashMap<String, Object>();
        orgMap.put(JsonKey.ORGANISATION_ID, null);
        innerMap.put(JsonKey.ORGANISATION, orgMap);
        reqObj.setRequest(innerMap);
        subject.tell(reqObj, probe.getRef());
        probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
    }
    
    @Test
    public void test26AddMemberToOrgExc(){
      TestKit probe = new TestKit(system);
      
      ActorRef subject = system.actorOf(props);

      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.ADD_MEMBER_ORGANISATION.getValue());
      HashMap<String, Object> innerMap = new HashMap<>();
      Map<String , Object> orgMap = new HashMap<String , Object>();
      orgMap.put(JsonKey.ORGANISATION_ID , "");
      orgMap.put(JsonKey.USER_ID, "");
      orgMap.put(JsonKey.ROLE,"");
      innerMap.put(JsonKey.ORGANISATION , orgMap);
      reqObj.setRequest(innerMap);
      subject.tell(reqObj, probe.getRef());
      probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
    }
    
    @Test
    public void test26AddMemberToOrgExc2(){
      TestKit probe = new TestKit(system);
      
      ActorRef subject = system.actorOf(props);

      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.ADD_MEMBER_ORGANISATION.getValue());
      HashMap<String, Object> innerMap = new HashMap<>();
      
      innerMap.put(JsonKey.ORGANISATION , null);
      reqObj.setRequest(innerMap);
      subject.tell(reqObj, probe.getRef());
      probe.expectMsgClass(ProjectCommonException.class);
    }
    
    @Test
    public void test27AddMemberToOrgSuc(){
      TestKit probe = new TestKit(system);
      
      ActorRef subject = system.actorOf(props);

      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.ADD_MEMBER_ORGANISATION.getValue());
      HashMap<String, Object> innerMap = new HashMap<>();

      Map<String , Object> orgMap = new HashMap<String , Object>();
      orgMap.put(JsonKey.ORGANISATION_ID , "");
      orgMap.put(JsonKey.USER_ID, "");
      orgMap.put(JsonKey.ROLE,"");
      innerMap.put(JsonKey.ORGANISATION , orgMap);
      reqObj.setRequest(innerMap);
      subject.tell(reqObj, probe.getRef());
      probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
    }

  @Test
  public void test27AddMemberToOrgSuc001(){
    TestKit probe = new TestKit(system);

    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.ADD_MEMBER_ORGANISATION.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.ORGANISATION_ID , orgId);
    innerMap.put(JsonKey.USER_ID , USER_ID+"01");
    innerMap.put(JsonKey.ROLE , "content-reviewer");

    reqObj.getRequest().put(JsonKey.USER_ORG , innerMap);
    reqObj.getRequest().put(JsonKey.REQUESTED_BY , "user1");
    subject.tell(reqObj, probe.getRef());
    Response resp = probe.expectMsgClass(duration("200 second"),Response.class);
  }

  @Test
  public void test27AddMemberToOrgSuc001AddAgain(){
    TestKit probe = new TestKit(system);

    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.ADD_MEMBER_ORGANISATION.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.ORGANISATION_ID , orgId);
    innerMap.put(JsonKey.USER_ID , USER_ID+"01");
    List<String> roles = new ArrayList<>();
    roles.add("PUBLIC");
    innerMap.put(JsonKey.ROLES , roles);
    innerMap.put(JsonKey.ROLE , "content-creator");

    reqObj.getRequest().put(JsonKey.USER_ORG , innerMap);
    reqObj.getRequest().put(JsonKey.REQUESTED_BY , "user1");

    subject.tell(reqObj, probe.getRef());
    Response resp = probe.expectMsgClass(duration("200 second"),Response.class);
  }
  
  @Test
  public void test27AddMemberToOrgSuc001AddAgain2(){
    TestKit probe = new TestKit(system);

    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.ADD_MEMBER_ORGANISATION.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.ORGANISATION_ID , orgId);
    innerMap.put(JsonKey.USER_ID , USER_ID+"01");
    
    innerMap.put(JsonKey.ROLE , "admin");

    reqObj.getRequest().put(JsonKey.USER_ORG , innerMap);
    reqObj.getRequest().put(JsonKey.REQUESTED_BY , "user1");

    subject.tell(reqObj, probe.getRef());
    Response resp = probe.expectMsgClass(duration("200 second"),Response.class);
  }
  
  @Test
  public void test27AddMemberToOrgSuc001AddAgain3(){
    TestKit probe = new TestKit(system);

    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.ADD_MEMBER_ORGANISATION.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.ORGANISATION_ID , orgId);
    innerMap.put(JsonKey.USER_ID , USER_ID+"01");
    
    innerMap.put(JsonKey.ROLE , "member");

    reqObj.getRequest().put(JsonKey.USER_ORG , innerMap);
    reqObj.getRequest().put(JsonKey.REQUESTED_BY , "user1");

    subject.tell(reqObj, probe.getRef());
    Response resp = probe.expectMsgClass(duration("200 second"),Response.class);
  }
  
  @Test
  public void test27AddMemberToOrgSuc001Exc(){
    TestKit probe = new TestKit(system);

    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.ADD_MEMBER_ORGANISATION.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.ORGANISATION_ID , orgId);
    innerMap.put(JsonKey.USER_ID , USER_ID+"01");
    List<String> roles = new ArrayList<>();
    roles.add("TEST");
    innerMap.put(JsonKey.ROLES , roles);
    innerMap.put(JsonKey.ROLE , "CONTENT_CREATOR");

    reqObj.getRequest().put(JsonKey.USER_ORG , innerMap);
    reqObj.getRequest().put(JsonKey.REQUESTED_BY , "user1");

    subject.tell(reqObj, probe.getRef());
    probe.expectMsgClass(ProjectCommonException.class);
  }

  @Test
  public void test27AddMemberToOrgExpUserIdNull(){
    TestKit probe = new TestKit(system);

    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.ADD_MEMBER_ORGANISATION.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.ORGANISATION_ID , orgId);
    innerMap.put(JsonKey.USER_ID , null);

    reqObj.getRequest().put(JsonKey.USER_ORG , innerMap);
    reqObj.getRequest().put(JsonKey.REQUESTED_BY , "user1");

    subject.tell(reqObj, probe.getRef());
    ProjectCommonException resp = probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
  }

  @Test
  public void test27AddMemberToOrgExpInvalidOrgId(){
    TestKit probe = new TestKit(system);

    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.ADD_MEMBER_ORGANISATION.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.ORGANISATION_ID , orgId+"udb932d");
    innerMap.put(JsonKey.USER_ID , USER_ID+"01");

    reqObj.getRequest().put(JsonKey.USER_ORG , innerMap);
    reqObj.getRequest().put(JsonKey.REQUESTED_BY , "user1");

    subject.tell(reqObj, probe.getRef());
    ProjectCommonException resp = probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
  }

  @Test
  public void test27AddMemberToOrgExpInvalidUserId(){
    TestKit probe = new TestKit(system);

    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.ADD_MEMBER_ORGANISATION.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.ORGANISATION_ID , orgId);
    innerMap.put(JsonKey.USER_ID , USER_ID+"01n49");

    reqObj.getRequest().put(JsonKey.USER_ORG , innerMap);
    reqObj.getRequest().put(JsonKey.REQUESTED_BY , "user1");

    subject.tell(reqObj, probe.getRef());
    ProjectCommonException resp = probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
  }
    
    @Test
    public void test28RemoveMemberFromOrgSuc001(){
      TestKit probe = new TestKit(system);
      
      ActorRef subject = system.actorOf(props);

      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.REMOVE_MEMBER_ORGANISATION.getValue());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.ORGANISATION_ID , orgId);
      innerMap.put(JsonKey.USER_ID , USER_ID+"01");

      reqObj.getRequest().put(JsonKey.USER_ORG , innerMap);
      reqObj.getRequest().put(JsonKey.REQUESTED_BY , "user1");

      subject.tell(reqObj, probe.getRef());
      Response resp = probe.expectMsgClass(duration("200 second"),Response.class);
    }

  @Test
  public void test28RemoveMemberFromOrgExpNullOrgId(){
    TestKit probe = new TestKit(system);

    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.REMOVE_MEMBER_ORGANISATION.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.ORGANISATION_ID , null);
    innerMap.put(JsonKey.USER_ID , USER_ID+"01");

    reqObj.getRequest().put(JsonKey.USER_ORG , innerMap);
    reqObj.getRequest().put(JsonKey.REQUESTED_BY , "user1");

    subject.tell(reqObj, probe.getRef());
    ProjectCommonException resp = probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
  }

  @Test
  public void test28RemoveMemberFromOrgExpNullUsrId(){
    TestKit probe = new TestKit(system);

    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.REMOVE_MEMBER_ORGANISATION.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.ORGANISATION_ID , orgId);
    innerMap.put(JsonKey.USER_ID , null);

    reqObj.getRequest().put(JsonKey.USER_ORG , innerMap);
    reqObj.getRequest().put(JsonKey.REQUESTED_BY , "user1");

    subject.tell(reqObj, probe.getRef());
    ProjectCommonException resp = probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
  }

  @Test
  public void test28RemoveMemberFromOrgExpInvalidRequestData(){
    TestKit probe = new TestKit(system);

    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.REMOVE_MEMBER_ORGANISATION.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.ORGANISATION_ID , orgId);
    innerMap.put(JsonKey.USER_ID , null);

    reqObj.getRequest().put(JsonKey.USER_ORG , null);
    reqObj.getRequest().put(JsonKey.REQUESTED_BY , "user1");

    subject.tell(reqObj, probe.getRef());
    ProjectCommonException resp = probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
  }
    
    @Test
    public void test29RemoveMemberFromOrgExc(){
      TestKit probe = new TestKit(system);
      
      ActorRef subject = system.actorOf(props);

      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.REMOVE_MEMBER_ORGANISATION.getValue());
      HashMap<String, Object> innerMap = new HashMap<>();
      Map<String , Object> orgMap = new HashMap<String , Object>();
      orgMap.put(JsonKey.ORGANISATION_ID , "");
      orgMap.put(JsonKey.USER_ID, "");
      innerMap.put(JsonKey.ORGANISATION , orgMap);
      reqObj.setRequest(innerMap);
      subject.tell(reqObj, probe.getRef());
      probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
    }
   
    @Test
    public void test30JoinMemberOrgSuc(){
      TestKit probe = new TestKit(system);
      
      ActorRef subject = system.actorOf(props);

      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.REMOVE_MEMBER_ORGANISATION.getValue());
      HashMap<String, Object> innerMap = new HashMap<>();
      Map<String , Object> orgMap = new HashMap<String , Object>();
      orgMap.put(JsonKey.ORGANISATION_ID , "");
      orgMap.put(JsonKey.USER_ID, "");
      innerMap.put(JsonKey.ORGANISATION , orgMap);
      reqObj.setRequest(innerMap);
      subject.tell(reqObj, probe.getRef());
      probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
    }
    
    @Test
    public void test31JoinMemberOrgExc(){
      TestKit probe = new TestKit(system);
      
      ActorRef subject = system.actorOf(props);

      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.REMOVE_MEMBER_ORGANISATION.getValue());
      HashMap<String, Object> innerMap = new HashMap<>();
      Map<String , Object> orgMap = new HashMap<String , Object>();
      orgMap.put(JsonKey.ORGANISATION_ID , "");
      orgMap.put(JsonKey.USER_ID, "");
      innerMap.put(JsonKey.ORGANISATION , orgMap);
      reqObj.setRequest(innerMap);
      subject.tell(reqObj, probe.getRef());
      probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
    }
    
    @Test
    public void test32ApproveMemberOrgSuc(){
      TestKit probe = new TestKit(system);
      
      ActorRef subject = system.actorOf(props);

      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.REMOVE_MEMBER_ORGANISATION.getValue());
      HashMap<String, Object> innerMap = new HashMap<>();
      Map<String , Object> orgMap = new HashMap<String , Object>();
      orgMap.put(JsonKey.ORGANISATION_ID , "");
      orgMap.put(JsonKey.USER_ID, "");
      innerMap.put(JsonKey.ORGANISATION , orgMap);
      reqObj.setRequest(innerMap);
      subject.tell(reqObj, probe.getRef());
      probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
    }
    
    @Test
    public void test33ApproveMemberFromOrgExc(){
      TestKit probe = new TestKit(system);
      
      ActorRef subject = system.actorOf(props);

      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.REMOVE_MEMBER_ORGANISATION.getValue());
      HashMap<String, Object> innerMap = new HashMap<>();
      Map<String , Object> orgMap = new HashMap<String , Object>();
      orgMap.put(JsonKey.ORGANISATION_ID , "");
      orgMap.put(JsonKey.USER_ID, "");
      innerMap.put(JsonKey.ORGANISATION , orgMap);
      reqObj.setRequest(innerMap);
      subject.tell(reqObj, probe.getRef());
      probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
    }
    
    @Test
    public void test34RejectMemberOrgSuc(){
      TestKit probe = new TestKit(system);
      
      ActorRef subject = system.actorOf(props);

      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.REMOVE_MEMBER_ORGANISATION.getValue());
      HashMap<String, Object> innerMap = new HashMap<>();
      Map<String , Object> orgMap = new HashMap<String , Object>();
      orgMap.put(JsonKey.ORGANISATION_ID , "");
      orgMap.put(JsonKey.USER_ID, "");
      innerMap.put(JsonKey.ORGANISATION , orgMap);
      reqObj.setRequest(innerMap);
      subject.tell(reqObj, probe.getRef());
      probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
    }
    
    @Test
    public void test35RejectMemberOrgExc(){
      TestKit probe = new TestKit(system);
      
      ActorRef subject = system.actorOf(props);

      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.REMOVE_MEMBER_ORGANISATION.getValue());
      HashMap<String, Object> innerMap = new HashMap<>();
      Map<String , Object> orgMap = new HashMap<String , Object>();
      orgMap.put(JsonKey.ORGANISATION_ID , "");
      orgMap.put(JsonKey.USER_ID, "");
      innerMap.put(JsonKey.ORGANISATION , orgMap);
      reqObj.setRequest(innerMap);
      subject.tell(reqObj, probe.getRef());
      probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
    }
    
    
    @Test
    public void test36CreateOrgType() {
      TestKit probe = new TestKit(system);
      ActorRef subject = system.actorOf(props);
      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.CREATE_ORG_TYPE.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(1);
      reqObj.getRequest().put(JsonKey.NAME, "ORG_TYPE_0001");
      subject.tell(reqObj, probe.getRef());
      Response response = probe.expectMsgClass(Response.class);
      assertEquals("SUCCESS", response.getResult().get(JsonKey.RESPONSE));
      
      Request req = new Request();
      req.setOperation(ActorOperations.GET_ORG_TYPE_LIST.getValue());
      req.setRequestId(ExecutionContext.getRequestId());
      req.setEnv(1);
      subject.tell(req, probe.getRef());
      Response res = probe.expectMsgClass(duration("200 second"), Response.class);
      List<Map<String,Object>> resMapList = (List<Map<String, Object>>) res.getResult().get(JsonKey.RESPONSE);
      if(null != resMapList && !resMapList.isEmpty()){
        for(Map<String,Object> map : resMapList){
          String name = (String) map.get(JsonKey.NAME);
          if(null != name && "ORG_TYPE_0001".equalsIgnoreCase(name)){
            orgTypeId1 = (String) map.get(JsonKey.ID);
          }
        }
      }
    }
    
    @Test
    public void test37CreateOrgType() {
      TestKit probe = new TestKit(system);
      ActorRef subject = system.actorOf(props);
      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.CREATE_ORG_TYPE.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(1);
      reqObj.getRequest().put(JsonKey.NAME, "ORG_TYPE_0002");
      subject.tell(reqObj, probe.getRef());
      Response response = probe.expectMsgClass(Response.class);
      assertEquals("SUCCESS", response.getResult().get(JsonKey.RESPONSE));
      
      Request req = new Request();
      req.setOperation(ActorOperations.GET_ORG_TYPE_LIST.getValue());
      req.setRequestId(ExecutionContext.getRequestId());
      req.setEnv(1);
      subject.tell(req, probe.getRef());
      Response res = probe.expectMsgClass(duration("200 second"), Response.class);
      List<Map<String,Object>> resMapList = (List<Map<String, Object>>) res.getResult().get(JsonKey.RESPONSE);
      if(null != resMapList && !resMapList.isEmpty()){
        for(Map<String,Object> map : resMapList){
          String name = (String) map.get(JsonKey.NAME);
          if(null != name && "ORG_TYPE_0002".equalsIgnoreCase(name)){
            orgTypeId2 = (String) map.get(JsonKey.ID);
          }
        }
      }
    }
    @Test
    public void test38OrgTypeList() {
      TestKit probe = new TestKit(system);
      ActorRef subject = system.actorOf(props);
      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.GET_ORG_TYPE_LIST.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(1);
      subject.tell(reqObj, probe.getRef());
      probe.expectMsgClass(duration("200 second"), Response.class);
    }
    
    @Test
    public void test39CreateOrgTypeWithSameName() {
      TestKit probe = new TestKit(system);
      ActorRef subject = system.actorOf(props);
      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.CREATE_ORG_TYPE.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(1);
      reqObj.getRequest().put(JsonKey.NAME, "ORG_TYPE_0001");
      subject.tell(reqObj, probe.getRef());
      probe.expectMsgClass(ProjectCommonException.class);
    }
    @Test
    public void test40UpdateOrgType() {
      TestKit probe = new TestKit(system);
      ActorRef subject = system.actorOf(props);
      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.UPDATE_ORG_TYPE.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(1);
      reqObj.getRequest().put(JsonKey.NAME, "ORG_TYPE_203");
      reqObj.getRequest().put(JsonKey.ID, orgTypeId1);
      subject.tell(reqObj, probe.getRef());
      Response response = probe.expectMsgClass(Response.class);
      assertEquals("SUCCESS", response.getResult().get(JsonKey.RESPONSE));
    }
    
    @Test
    public void test41UpdateOrgTypeWithExistingName() {
      TestKit probe = new TestKit(system);
      ActorRef subject = system.actorOf(props);
      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.UPDATE_ORG_TYPE.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(1);
      reqObj.getRequest().put(JsonKey.NAME, "ORG_TYPE_0002");
      reqObj.getRequest().put(JsonKey.ID, orgTypeId1);
      subject.tell(reqObj, probe.getRef());
      ProjectCommonException response = probe.expectMsgClass(duration("200 second"),ProjectCommonException.class);
    }
    
    @Test
    public void test42UpdateOrgTypeWithWrongId() {
      TestKit probe = new TestKit(system);
      ActorRef subject = system.actorOf(props);
      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.UPDATE_ORG_TYPE.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(1);
      reqObj.getRequest().put(JsonKey.NAME, "ORG_TYPE_12");
      String id = orgTypeId2+"1";
      reqObj.getRequest().put(JsonKey.ID, id);
      subject.tell(reqObj, probe.getRef());
      Response response = probe.expectMsgClass(duration("200 second"),Response.class);
      assertEquals("SUCCESS", response.getResult().get(JsonKey.RESPONSE));
    }
    
    
    @AfterClass
    public static void delete() {
      System.out.println("After class");
     /* SSOManager ssoManager = SSOServiceFactory.getInstance();
      Map<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.USER_ID, usrId);
      ssoManager.removeUser(innerMap);*/
      try{
      CassandraOperation operation = ServiceFactory.getInstance();
      operation.deleteRecord(orgTypeDbInfo.getKeySpace(), orgTypeDbInfo.getTableName(), orgTypeId1);
      operation.deleteRecord(orgTypeDbInfo.getKeySpace(), orgTypeDbInfo.getTableName(), orgTypeId2);
      //operation.deleteRecord(userManagementDB.getKeySpace(), userManagementDB.getTableName(), usrId);
      operation.deleteRecord(addressDB.getKeySpace(), addressDB.getTableName(), addressId);
      operation.deleteRecord(orgDB.getKeySpace(), orgDB.getTableName(), orgId);
      operation.deleteRecord(orgDB.getKeySpace(), orgDB.getTableName(), HASH_TAG_ID);
      operation.deleteRecord(orgDB.getKeySpace(), orgDB.getTableName(), parentOrgId);
      operation.deleteRecord(locationDB.getKeySpace(), locationDB.getTableName(), LOCATION_ID);
      System.out.println("1 "+ orgId);
      
      operation.deleteRecord(orgDB.getKeySpace(), orgDB.getTableName(), OrgIDWithoutSourceAndExternalId);
      System.out.println("2 "+ OrgIDWithoutSourceAndExternalId);
      
      operation.deleteRecord(orgDB.getKeySpace(), orgDB.getTableName(), OrgIdWithSourceAndExternalId);
      System.out.println("3 "+ OrgIdWithSourceAndExternalId);
      
      }catch(Throwable th){
        th.printStackTrace();
      }
      if(!ProjectUtil.isStringNullOREmpty(usrId)){
        ElasticSearchUtil.removeData(ProjectUtil.EsIndex.sunbird.getIndexName(),
            ProjectUtil.EsType.user.getTypeName(), usrId);
      }

      if(!ProjectUtil.isStringNullOREmpty(USER_ID)){
        ElasticSearchUtil.removeData(ProjectUtil.EsIndex.sunbird.getIndexName(),
            ProjectUtil.EsType.user.getTypeName(), USER_ID);
      }

      if(!ProjectUtil.isStringNullOREmpty(USER_ID+"01")){
        ElasticSearchUtil.removeData(ProjectUtil.EsIndex.sunbird.getIndexName(),
            ProjectUtil.EsType.user.getTypeName(), USER_ID+"01");
      }

      if(!ProjectUtil.isStringNullOREmpty(orgId)) {
        ElasticSearchUtil.removeData(ProjectUtil.EsIndex.sunbird.getIndexName(),
            ProjectUtil.EsType.organisation.getTypeName(), orgId);
      }
      if(!ProjectUtil.isStringNullOREmpty(OrgIDWithoutSourceAndExternalId)) {
        ElasticSearchUtil.removeData(ProjectUtil.EsIndex.sunbird.getIndexName(),
            ProjectUtil.EsType.organisation.getTypeName(), OrgIDWithoutSourceAndExternalId);
      }
      if(!ProjectUtil.isStringNullOREmpty(OrgIdWithSourceAndExternalId)) {
          ElasticSearchUtil.removeData(ProjectUtil.EsIndex.sunbird.getIndexName(),
              ProjectUtil.EsType.organisation.getTypeName(), OrgIdWithSourceAndExternalId);
        }

      Map<String, Object> dbMap = new HashMap<>();
      dbMap.put(JsonKey.PROVIDER, PROVIDER);
      dbMap.put(JsonKey.EXTERNAL_ID, EXTERNAL_ID);
      Response result = operation.getRecordsByProperties(orgDB.getKeySpace(),
          orgDB.getTableName(), dbMap);
      List<Map<String, Object>> list = (List<Map<String, Object>>) result.get(JsonKey.RESPONSE);
      if (!(list.isEmpty())) {
        for (Map<String, Object> res : list) {
          String id = (String) res.get(JsonKey.ID);
          System.out.println("ID is " + id);
          operation.deleteRecord(orgDB.getKeySpace(), orgDB.getTableName(), id);
          ElasticSearchUtil.removeData(ProjectUtil.EsIndex.sunbird.getIndexName(),
              ProjectUtil.EsType.organisation.getTypeName(), id);
        }
      }
    try{
      dbMap = new HashMap<>();
      dbMap.put(JsonKey.HASHTAGID, HASH_TAG_ID);
      //dbMap.put(JsonKey.EXTERNAL_ID, EXTERNAL_ID);
      result = operation.getRecordsByProperties(orgDB.getKeySpace(),
          orgDB.getTableName(), dbMap);
      list = (List<Map<String, Object>>) result.get(JsonKey.RESPONSE);
      if (!(list.isEmpty())) {
        for (Map<String, Object> res : list) {
          String id = (String) res.get(JsonKey.ID);
          System.out.println("ID is " + id);
          operation.deleteRecord(orgDB.getKeySpace(), orgDB.getTableName(), id);
          ElasticSearchUtil.removeData(ProjectUtil.EsIndex.sunbird.getIndexName(),
              ProjectUtil.EsType.organisation.getTypeName(), id);
        }
      }
    }catch(Exception e){}
      dbMap = new HashMap<>();
      dbMap.put(JsonKey.CHANNEL, CHANNEL);
      //dbMap.put(JsonKey.EXTERNAL_ID, EXTERNAL_ID);
      result = operation.getRecordsByProperties(orgDB.getKeySpace(),
          orgDB.getTableName(), dbMap);
      list = (List<Map<String, Object>>) result.get(JsonKey.RESPONSE);
      if (!(list.isEmpty())) {
        for (Map<String, Object> res : list) {
          String id = (String) res.get(JsonKey.ID);
          System.out.println("ID is " + id);
          operation.deleteRecord(orgDB.getKeySpace(), orgDB.getTableName(), id);
          ElasticSearchUtil.removeData(ProjectUtil.EsIndex.sunbird.getIndexName(),
              ProjectUtil.EsType.organisation.getTypeName(), id);
        }
      }

      dbMap = new HashMap<>();
      dbMap.put(JsonKey.ORGANISATION_ID, orgId);
      //dbMap.put(JsonKey.EXTERNAL_ID, EXTERNAL_ID);
      result = operation.getRecordsByProperties(userOrgDbInfo.getKeySpace(),
          userOrgDbInfo.getTableName(), dbMap);
      list = (List<Map<String, Object>>) result.get(JsonKey.RESPONSE);
      if (!(list.isEmpty())) {
        for (Map<String, Object> res : list) {
          String id = (String) res.get(JsonKey.ID);
          System.out.println("ID is " + id);
          operation.deleteRecord(orgDB.getKeySpace(), orgDB.getTableName(), id);
          ElasticSearchUtil.removeData(ProjectUtil.EsIndex.sunbird.getIndexName(),
              ProjectUtil.EsType.organisation.getTypeName(), id);
        }
      }

      dbMap = new HashMap<>();
      dbMap.put(JsonKey.ORGANISATION_ID, orgId);
      //dbMap.put(JsonKey.EXTERNAL_ID, EXTERNAL_ID);
      result = operation.getRecordsByProperties(userOrgDbInfo.getKeySpace(),
          userOrgDbInfo.getTableName(), dbMap);
      list = (List<Map<String, Object>>) result.get(JsonKey.RESPONSE);
      if (!(list.isEmpty())) {
        for (Map<String, Object> res : list) {
          String id = (String) res.get(JsonKey.ID);
          System.out.println("ID is " + id);
          operation.deleteRecord(orgDB.getKeySpace(), orgDB.getTableName(), id);
          ElasticSearchUtil.removeData(ProjectUtil.EsIndex.sunbird.getIndexName(),
              ProjectUtil.EsType.organisation.getTypeName(), id);
        }
      }

      dbMap = new HashMap<>();
      dbMap.put(JsonKey.USER_ID, USER_ID);
      //dbMap.put(JsonKey.EXTERNAL_ID, EXTERNAL_ID);
      result = operation.getRecordsByProperties(userOrgDbInfo.getKeySpace(),
          userOrgDbInfo.getTableName(), dbMap);
      list = (List<Map<String, Object>>) result.get(JsonKey.RESPONSE);
      if (!(list.isEmpty())) {
        for (Map<String, Object> res : list) {
          String id = (String) res.get(JsonKey.ID);
          System.out.println("ID is " + id);
          operation.deleteRecord(userOrgDbInfo.getKeySpace(), userOrgDbInfo.getTableName(), id);
          /*ElasticSearchUtil.removeData(ProjectUtil.EsIndex.sunbird.getIndexName(),
              ProjectUtil.EsType.organisation.getTypeName(), id);*/
        }
      }
    }
}