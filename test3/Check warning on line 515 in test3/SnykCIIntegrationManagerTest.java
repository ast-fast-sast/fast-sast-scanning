package com.salesforce.ast.oss.snykci;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.salesforce.ast.oss.snykci.pojo.*;
import com.salesforce.ast.sast.scm.git.GitSomaManager;
import com.salesforce.ast.oss.cs.reports.ReportUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.salesforce.ast.config.MasterConfig;
import com.salesforce.ast.oss.Util;
import com.salesforce.ast.oss.metrics.OSSMetricsManager;
import com.salesforce.ast.oss.snykci.dao.SnykFailuresDAO;
import com.salesforce.ast.oss.snykci.dao.SnykOrgMappingsDAO;
import com.salesforce.ast.oss.snykci.git.AstSnykCIGit;
import com.salesforce.ast.oss.snykci.snyk.AstSnyk;
import com.salesforce.ast.s3.S3Store;
import com.salesforce.ast.util.PasswordManager;
import com.salesforce.sds.core.script.ScriptExecutor;


public class SnykCIIntegrationManagerTest {
	@Mock
	Logger logger;

	@Mock
	PasswordManager passwordManager;

	@Mock
	AstSnyk astSnyk;

	@Mock
	AstSnykCIGit astSnykCIGit;

	@Mock
	MasterConfig masterConfig;

	@Mock
	SnykOrgMappingsDAO snykOrgMappingsDAO;

	@Mock
	ScriptExecutor scriptExecutor;

	@InjectMocks
	SnykCiIntegrationManager snykCiIntegrationManager;

	@Mock
	OSSMetricsManager OSSMetricsManager;

	@Mock
	SnykFailuresDAO snykFailuresDAO;

	@Mock
	SnykCiIntegrationReportGenerator snykCiIntegrationReportGenerator;

	@Mock
	SnykCiPublishReport snykCiPublishReport;

	@Mock
	RepoImportManager repoImportManager;

	@Mock
	ReportUtils reportUtils;

	@Mock
	S3Store s3Store;

	@Mock
	GitSomaManager gitSomaManager;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		when(masterConfig.getOrgUsersyncSleepTime()).thenReturn(0);
	}

	@Test
	public void testGetSnykOrgIdWithoutInput() throws Exception {
		try {
			String result = snykCiIntegrationManager.getSnykOrgId(null, null, null, null);
			Assert.fail();
		} catch (Exception e) {
		}
	}

	@Test
	public void testGetSnykOrgIdWithInput() throws Exception {
		when(astSnykCIGit.getGitOrgIdFromGitSoma("NewOrg", ScmType.GITSOMA.toString())).thenReturn("gitOrgId");
		// when(astSnyk.createSnykOrg(any(), any(), any(),
		// any())).thenReturn("gitOrgId1");

		SnykOrgMappingRecord snykOrgMappingRecord = new SnykOrgMappingRecord();
		snykOrgMappingRecord.setSnykOrgId("snykOrgId");

		when(snykOrgMappingsDAO.getSnykOrgIdMapingRecord("gitOrgId", ScmType.GITSOMA.toString()))
				.thenReturn(snykOrgMappingRecord);

		try {
			String result = snykCiIntegrationManager.getSnykOrgId("NewOrg", null, null, ScmType.GITSOMA.toString());
		} catch (Exception e) {
			Assert.assertEquals(e.getMessage(), "UNKNOWN");
		}
	}

	@Test
	public void testGetSnykOrgIdWithDBFetchError() throws Exception {
		when(astSnykCIGit.getGitOrgIdFromGitSoma("NewOrg", ScmType.GITSOMA.toString())).thenReturn("gitOrgId");

		SnykOrgMappingRecord snykOrgMappingRecord = null;

		when(snykOrgMappingsDAO.getSnykOrgIdMapingRecord("gitOrgId", ScmType.GITSOMA.toString()))
				.thenReturn(snykOrgMappingRecord);

		Set<GitUser> gitOrgMembers = new HashSet<>();
		GitUser gitUser = new GitUser();
		gitUser.setId("id");
		gitUser.setLogin("loginId");
		gitOrgMembers.add(gitUser);

		when(astSnykCIGit.getGitOrgReposMembers("NewOrg", ScmType.GITSOMA.toString())).thenReturn(gitOrgMembers);

		try {
			String result = snykCiIntegrationManager.getSnykOrgId("NewOrg", null, null, ScmType.GITSOMA.toString());
		} catch (Exception e) {
			Assert.assertEquals(e.getMessage(), "UNKNOWN");
		}
	}

	@Test
	public void testGetSnykOrgIdWithDBResult() throws Exception {
		when(astSnykCIGit.getGitOrgIdFromGitSoma("NewOrg", ScmType.GITSOMA.toString())).thenReturn("gitOrgId");
		// when(astSnyk.createSnykOrg(any(), any(), any(),
		// any())).thenReturn("gitOrgId1");

		SnykOrgMappingRecord snykOrgMappingRecord = new SnykOrgMappingRecord();
		snykOrgMappingRecord.setSnykOrgId("snykOrgId");

		when(snykOrgMappingsDAO.getSnykOrgIdMapingRecord("gitOrgId", ScmType.GITSOMA.toString()))
				.thenReturn(snykOrgMappingRecord);

		try {
			String result = snykCiIntegrationManager.getSnykOrgId("NewOrg", null, null, ScmType.GITSOMA.toString());
		} catch (Exception e) {
			Assert.assertEquals(e.getMessage(), "UNKNOWN");
		}
	}

	@Test
	public void testGetSnykOrgIdWithDBResult1() throws Exception {
		when(astSnykCIGit.getGitOrgIdFromGitSoma("NewOrg", ScmType.GITSOMA.toString())).thenReturn("gitOrgId");
		org.mockito.BDDMockito.doThrow(new JsonGenerationException("error")).when(astSnyk).createSnykOrg(any(), any(),
				any(), any());

		SnykOrgMappingRecord snykOrgMappingRecord = new SnykOrgMappingRecord();
		snykOrgMappingRecord.setSnykOrgId("snykOrgId");

		when(snykOrgMappingsDAO.getSnykOrgIdMapingRecord("gitOrgId", ScmType.GITSOMA.toString()))
				.thenReturn(snykOrgMappingRecord);

		try {
			String result = snykCiIntegrationManager.getSnykOrgId("NewOrg", null, null, ScmType.GITSOMA.toString());
		} catch (Exception e) {
			Assert.assertEquals(e.getMessage(), "error");
		}
	}

	@Test
	public void onboardedOrgs() throws Exception {
		List<String> orgList = new ArrayList<>();
		orgList.add("testOrg");

		when(snykOrgMappingsDAO.getOnboardedGitOrgs(ScmType.GITSOMA.toString(), 1)).thenReturn(orgList);

		List<String> result = snykCiIntegrationManager.onboardedOrgs(ScmType.GITSOMA.toString(), 1);
		Assert.assertEquals(result.get(0), orgList.get(0));
	}

	@Test
	public void onboardedOrgs2() throws Exception {
		when(snykOrgMappingsDAO.getOnboardedGitOrgs(ScmType.GITSOMA.toString(), 1)).thenReturn(null);
		try {
			List<String> result = snykCiIntegrationManager.onboardedOrgs(ScmType.GITSOMA.toString(), 1);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals(e.getMessage(), SnykCIIntegrationConstants.NOT_FOUND);
		}
	}

	@Test
	public void onboardedOrgs3() throws Exception {
		List<String> orgList = new ArrayList<>();
		when(snykOrgMappingsDAO.getOnboardedGitOrgs(ScmType.GITSOMA.toString(), 1)).thenReturn(orgList);
		try {
			List<String> result = snykCiIntegrationManager.onboardedOrgs(ScmType.GITSOMA.toString(), 1);
		} catch (Exception e) {
			Assert.assertEquals(e.getMessage(), SnykCIIntegrationConstants.NOT_FOUND);
		}
	}

	@Test
	public void gitSnykUserSync() throws Exception {
		try {
			Set<GitUser> gitOrgMembers = new HashSet<>();
			GitUser gitUser = new GitUser();
			gitUser.setId("id");
			gitUser.setLogin("loginId");
			gitOrgMembers.add(gitUser);
			when(astSnykCIGit.getGitOrgReposMembers("NewOrg", ScmType.GITSOMA.toString())).thenReturn(gitOrgMembers);
			snykCiIntegrationManager.gitSnykUserSync("testOrg", ScmType.GITSOMA.toString(), "CI");
		} catch (Exception e) {
			Assert.fail();
		}
	}

	@Test
	public void gitSnykUserSync2() throws Exception {
		try {
			when(astSnykCIGit.getGitOrgReposMembers("NewOrg", ScmType.GITSOMA.toString())).thenReturn(null);
			snykCiIntegrationManager.gitSnykUserSync("testOrg", ScmType.GITSOMA.toString(), "CI");
		} catch (Exception e) {
			Assert.fail();
		}
	}

	@Test
	public void gitGroupSnykUserSyncTest() throws Exception {
		try {
			when(snykOrgMappingsDAO.getOnboardedGitOrgs(ScmType.GITSOMA.toString(), 1)).thenReturn(null);
			snykCiIntegrationManager.gitGroupSnykUserSync(ScmType.GITSOMA.toString(), "CI");
		} catch (Exception e) {
			Assert.fail();
		}
	}
	
	@Test
	public void gitGroupSnykUserSyncTest7() throws Exception {
		try {
			when(snykOrgMappingsDAO.getOnboardedGitOrgs(ScmType.GITSOMA.toString(), 1)).thenReturn(null);
			
			ObjectMapper mapper = Util.getObjectMapper();
			JsonNode node1 = mapper.readTree("{\"orgs\":[{\"name\":\"GITHUB/org1\"},{\"name\":\"GITSOMA/org2\"},{\"name\":\"org3\"}]}");
			ArrayNode orgs = (ArrayNode) node1.get("orgs");
			
			when(astSnyk.getOrgs(SnykCIIntegrationConstants.SNYKTOKEN)).thenReturn(orgs);
			snykCiIntegrationManager.gitGroupSnykUserSync(ScmType.GITSOMA.toString(), "SCM");
			
			snykCiIntegrationManager.gitGroupSnykUserSync(ScmType.GITSOMA.toString(), "SCM");
			
		} catch (Exception e) {
			Assert.fail();
		}
	}

	@Test
	public void gitGroupSnykUserSyncTest5() throws Exception {
		try {
			List<String> orgList = new ArrayList<>();
			when(snykOrgMappingsDAO.getOnboardedGitOrgs(ScmType.GITSOMA.toString(), 1)).thenReturn(orgList);
			snykCiIntegrationManager.gitGroupSnykUserSync(ScmType.GITSOMA.toString(), "CI");
		} catch (Exception e) {
			Assert.fail();
		}
	}

	@Test
	public void gitGroupSnykUserSyncTest2() throws Exception {
		try {	
			String = "sample-test Line";
			when(snykOrgMappingsDAO.getOnboardedGitOrgs(ScmType.GITSOMA.toString(), 1)).thenThrow(new Exception("NEW"));
			snykCiIntegrationManager.gitGroupSnykUserSync(ScmType.GITSOMA.toString(), "CI");
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals(e.getMessage(), "NEW");
		}
	}

	@Test
	public void gitGroupSnykUserSyncTest3() throws Exception {
		try {
			List<String> orgList = new ArrayList<>();
			for (int i = 0; i < 100; i++) {
				orgList.add("THIS_IS_NOT_REAL_ORG" + i);
			}
			when(snykOrgMappingsDAO.getOnboardedGitOrgs(ScmType.GITSOMA.toString(), 1)).thenReturn(orgList);
			snykCiIntegrationManager.gitGroupSnykUserSync(ScmType.GITSOMA.toString(), "CI");
		} catch (Exception e) {
			Assert.fail();
		}
	}

	@Test
	public void gitGroupSnykUserSyncTest4() throws Exception {
		try {
			List<String> orgList = new ArrayList<>();
			orgList.add("THIS_IS_NOT_REAL_ORG");
			orgList.add("THIS_IS_NOT_REAL_ORG1");
			when(snykOrgMappingsDAO.getOnboardedGitOrgs(ScmType.GITSOMA.toString(), 1)).thenReturn(orgList);
			snykCiIntegrationManager.gitGroupSnykUserSync(ScmType.GITSOMA.toString(), "CI");
		} catch (Exception e) {
			Assert.fail();
		}
	}

	@Test
	public void deleteSnykOrgTest() throws Exception {
		try {
			SnykOrgMappingRecord record = new SnykOrgMappingRecord();
			record.setSnykOrgId("snykOrgId");
			List<String> orgList = new ArrayList<>();
			orgList.add("THIS_IS_NOT_REAL_ORG");
			when(snykOrgMappingsDAO.getSnykOrgIdMapingRecord(null, ScmType.GITSOMA.toString())).thenReturn(record);
			when(snykOrgMappingsDAO.getOnboardedGitOrgs(ScmType.GITSOMA.toString(), 1)).thenReturn(orgList);
			boolean flag = snykCiIntegrationManager.deleteSnykOrg("SnykTestOrg", ScmType.GITSOMA.toString());
			Assert.assertEquals(true, flag);
		} catch (Exception e) {
			Assert.fail();
		}
	}

	@Test
	public void deleteSnykOrgTest2() throws Exception {
		try {
			when(snykOrgMappingsDAO.getSnykOrgIdMapingRecord(null, ScmType.GITSOMA.toString()))
					.thenThrow(new Exception());
			boolean flag = snykCiIntegrationManager.deleteSnykOrg("SnykTestOrg", ScmType.GITSOMA.toString());
			Assert.assertEquals(false, flag);
		} catch (Exception e) {
			Assert.fail();
		}
	}

	@Test
	public void deleteSnykOrgTest1() throws Exception {
		try {
			SnykOrgMappingRecord record = new SnykOrgMappingRecord();
			record.setGitOrgId("id");
			when(snykOrgMappingsDAO.getSnykOrgIdMapingRecord("SnykTestOrg", ScmType.GITSOMA.toString()))
					.thenReturn(record);
			boolean flag = snykCiIntegrationManager.deleteSnykOrg("SnykTestOrg", ScmType.GITSOMA.toString());
			Assert.assertEquals(true, flag);
		} catch (Exception e) {
			Assert.fail();
		}
	}

	@Test
	public void getSnykFailuresTest() throws Exception {
		List<SnykFailuresRecord> list = new ArrayList<>();
		SnykFailuresRecord record = new SnykFailuresRecord();
		record.setGitOrgId("id");
		list.add(record);
		try {
			when(snykFailuresDAO.getOnboardedGitOrgs(1)).thenReturn(list);
			List<SnykFailuresRecord> list1 = snykCiIntegrationManager.getSnykFailures(1);
			Assert.assertEquals(list, list1);
		} catch (Exception e) {
			Assert.fail();
		}
	}

	@Test
	public void createOrgIdMappingToStorageTest() throws Exception {
		try {
			snykCiIntegrationManager.createOrgIdMappingToStorage("name", "slug", "id", "id", "GITSOMA");
		} catch (Exception e) {
			Assert.fail();
		}
	}

	@Test
	public void testGenerateSnykIssueReport() throws Exception {
		snykCiIntegrationManager.generateSnykIssueReport("testOrg");
	}

	@Test
	public void testFixPRReportGenerator() throws Exception {
		snykCiIntegrationManager.fixPRReportGenerator("testOrg");
	}
	
	@Test
	public void testGetRateLimit() throws Exception {
		try {
			snykCiIntegrationManager.getGitRateLimit();
		} catch (Exception e) {
			Assert.fail();
		}
	}
	
	@Test
	public void testprintSnykPRCount() throws Exception {
		try {
			snykCiIntegrationManager.printSnykPRCount();
		} catch (Exception e) {
			Assert.fail();
		}
	}

	@Test
	public void testGetFalconRepoSetException() throws Exception {
		try {
			snykCiIntegrationManager.projectTagSync();
		} catch (Exception e) {
			Assert.fail();
		}
	}

	@Test
	public void testGetFalconRepoSet() throws Exception {
		when(s3Store.getObject(any(), any())).thenReturn("{\"records\":[\"org/repo\"]}");
		Map<String, SnykProject> projectMap = new HashMap<String, SnykProject>();
		SnykProject snykProject = new SnykProject("repo", "org", "id", "tag", "url");
		projectMap.put("someId", snykProject);
		when(astSnyk.getProjectToRepoMap(any(), any(), any(Boolean.class))).thenReturn(projectMap);

		try {
			snykCiIntegrationManager.projectTagSync();
		} catch (Exception e) {
			Assert.fail();
		}
	}
	
	@Test
	public void testFinalize() {
		snykCiIntegrationManager.finalize();
	}
	
	@Test
	public void testprintSnykPRCountEx01() throws Exception {
		when(snykOrgMappingsDAO.getSnykOrgIds(any())).thenThrow(new Exception());
		try {
			snykCiIntegrationManager.printSnykPRCount();
		} catch (Exception e) {
			Assert.fail();
		}
	}
	
	@Test
	public void testprintSnykPRCountEx02() throws Exception {
		when(astSnyk.getSnykOrgIdsByNames(any(), any())).thenThrow(new Exception());
		try {
			snykCiIntegrationManager.printSnykPRCount();
		} catch (Exception e) {
			Assert.fail();
		}
	}

	@Test
	public void testOnboardUser() throws Exception {
		String s = "[{\n" +
				"\"id\": \"0ff64d0a-d8ad-4717-b37d-26da1ffc3bf5\",\n" +
				"\"name\": \"GITSOMA/GRC-Operation\",\n" +
				"\"slug\": \"gitsomagrc-operation\",\n" +
				"\"url\": \"https://app.snyk.io/org/gitsomagrc-operation\",\n" +
				"\"group\": {\n" +
				"\"name\": \"SFDC\",\n" +
				"\"id\": \"6778547c-2c44-4b9b-81a4-a14c47b55302\"\n" +
				"}\n" +
				"}]";
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		JsonNode node1 = mapper.readTree(s);
		ArrayNode a =  (ArrayNode) node1;
		when(astSnyk.getOrgs(anyString())).thenReturn(a);
		when(gitSomaManager.getFileContentAsString(anyString(),anyString(),anyString())).thenReturn("[\n" +
				"  {\n" +
				"    \"email\": \"praveen.tripathi@salesforce.com\",\n" +
				"    \"snykGroup\": \"SFDC\",\n" +
				"    \"snykOrg\": \"GITSOMA/Infrastructure Security\",\n" +
				"    \"gitOrg\": \"Infrastructure Security\"\n" +
				"  },\n" +
				"  {\n" +
				"    \"email\": \"akshay.kumar1@salesforce.com\",\n" +
				"    \"snykGroup\": \"SFDC\",\n" +
				"    \"snykOrg\": \"GITSOMA/GRC-Operation\",\n" +
				"    \"groupName\": \"SFDC\"\n" +
				"    \"gitOrg\": \"Infrastructure Security\"\n" +
				"  }\n" +
				"]");
		when(gitSomaManager.writeFile(anyString(),anyString(),anyString(),anyString(),anyString())).thenReturn(true);
		boolean b = snykCiIntegrationManager.onBoardUser();
	}

	@Test
	public void testOnboardUser1() throws Exception {
		String s = "[{\n" +
				"\"id\": \"0ff64d0a-d8ad-4717-b37d-26da1ffc3bf5\",\n" +
				"\"name\": \"GITSOMA/GRC-Operation\",\n" +
				"\"slug\": \"gitsomagrc-operation\",\n" +
				"\"url\": \"https://app.snyk.io/org/gitsomagrc-operation\",\n" +
				"\"group\": {\n" +
				"\"name\": \"SFDC\",\n" +
				"\"id\": \"6778547c-2c44-4b9b-81a4-a14c47b55302\"\n" +
				"}\n" +
				"}]";
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		JsonNode node1 = mapper.readTree(s);
		ArrayNode a =  (ArrayNode) node1;
		when(astSnyk.getOrgs(anyString())).thenReturn(a);
		when(gitSomaManager.getFileContentAsString(anyString(),anyString(),anyString())).thenReturn("[\n" +
				"  {\n" +
				"    \"email\": \"praveen.tripathi@salesforce.com\",\n" +
				"    \"snykGroup\": \"SFDC\",\n" +
				"    \"snykOrg\": \"GITSOMA/Infrastructure Security\",\n" +
				"    \"gitOrg\": \"Infrastructure Security\"\n" +
				"  },\n" +
				"  {\n" +
				"    \"email\": \"akshay.kumar1@salesforce.com\",\n" +
				"    \"snykGroup\": \"SFDC\",\n" +
				"    \"snykOrg\": \"GITSOMA/Infrastructure Security\",\n" +
				"    \"gitOrg\": \"Infrastructure Security\"\n" +
				"  }\n" +
				"]");
		when(gitSomaManager.writeFile(anyString(),anyString(),anyString(),anyString(),anyString())).thenThrow(new RuntimeException());
		boolean b = snykCiIntegrationManager.onBoardUser();
	}

	@Test
	public void testOnboardUser2() throws Exception {
		String s = "[{\n" +
				"\"id\": \"0ff64d0a-d8ad-4717-b37d-26da1ffc3bf5\",\n" +
				"\"name\": \"GITSOMA/GRC-Operation\",\n" +
				"\"slug\": \"gitsomagrc-operation\",\n" +
				"\"url\": \"https://app.snyk.io/org/gitsomagrc-operation\",\n" +
				"\"group\": {\n" +
				"\"name\": \"SFDC\",\n" +
				"\"id\": \"6778547c-2c44-4b9b-81a4-a14c47b55302\"\n" +
				"}\n" +
				"}]";
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		JsonNode node1 = mapper.readTree(s);
		ArrayNode a =  (ArrayNode) node1;
		when(astSnyk.getOrgs(anyString())).thenReturn(a);
		when(gitSomaManager.getFileContentAsString(anyString(),anyString(),anyString())).thenThrow(new RuntimeException());
		when(gitSomaManager.writeFile(anyString(),anyString(),anyString(),anyString(),anyString())).thenThrow(new RuntimeException());
		boolean b = snykCiIntegrationManager.onBoardUser();
	}

	@Test
	public void testOnboardUser3() throws Exception {
		when(astSnyk.getOrgs(anyString())).thenReturn(null);
		boolean b = snykCiIntegrationManager.onBoardUser();
	}
	@Test
	public void testImportRepos1() throws Exception {
		Mockito.when(astSnyk.getSnykOrgIntData(anyString())).thenReturn("sjhf3497yt983ywefb");
		Map<String,String> repoMap = new HashMap<String, String>();
		repoMap.put("astmaster","master");
		repoMap.put("astworker","master");
		when(repoImportManager.getImportJson(any(),any(),any(),any(),any(),any(),any(),anyInt())).thenReturn(repoMap);
		when(gitSomaManager.writeFile(anyString(),anyString(),anyString(),anyString(),anyString())).thenReturn(true);
		when(masterConfig.getOssScanDirectory()).thenReturn("src/test/resources");
		boolean res = snykCiIntegrationManager.importRepos("testOrgId1","testGitOrg2","testServiceAccount","GITSOMA",null,-1);
		Assert.assertFalse(res);
		snykCiIntegrationManager.cleanUp("src/test/resourcestestOrgId1");
		File file = new File("src/test/resourcestestOrgId1");
		file.delete();
	}

	@Test
	public void testImportRepos2() throws Exception {
		Mockito.when(astSnyk.getSnykOrgIntData(anyString())).thenReturn(null);
		Map<String,String> repoMap = new HashMap<String, String>();
		repoMap.put("astmaster","master");
		repoMap.put("astworker","master");
		when(repoImportManager.getImportJson(any(),any(),any(),any(),any(),any(),any(),anyInt())).thenReturn(repoMap);
		when(masterConfig.getOssScanDirectory()).thenReturn("src/test/resources");
		List<Repo> repos = new ArrayList<>();
		Repo repo = new Repo();
		repo.setGitRepoName("infraSec");
		repo.setGitBranchName("master");
		repos.add(repo);
		boolean res = snykCiIntegrationManager.importRepos("testOrgId2","testGitOrg2","testServiceAccount","GITSOMA",repos,-1);
		Assert.assertFalse(res);
		snykCiIntegrationManager.cleanUp("src/test/resourcestestOrgId2");
		File file = new File("src/test/resourcestestOrgId2");
		file.delete();
	}

	@Test
	public void executeOnboardRepoTaskTest() throws Exception {
		String s = "[{\n" +
				"\"gitOrgName\" : \"testOrg\",\n" +
				"\"snykOrgId\" : \"whiufhasdklnadllk\",\n" +
				"\"repos\": [{\"gitRepoName\" : \"testRepo1\",\n" +
				"\"gitBranchName\" : \"testBranch1\"},\n" +
				"{\"gitRepoName\" : \"testRepo2\",\n" +
				"\"gitBranchName\" : \"testBranch2\"}],\n" +
				"\"scmType\" : \"GITHUB\"\n" +
				"}]";
		String sampleLine2 = "Another sample test line";
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		//when(astSnyk.getOrgs(anyString())).thenReturn(a);
		SnykOrgMappingRecord snykOrgMappingRecord = new SnykOrgMappingRecord();
		snykOrgMappingRecord.setSnykOrgId("testSnykOrg");
		when(snykOrgMappingsDAO.getSnykOrgIdByGitOrgName(anyString(),anyString())).thenReturn(snykOrgMappingRecord);
		when(astSnyk.getSnykOrgIntData(anyString())).thenReturn("3465436343646343243");
		when(gitSomaManager.getFileContentAsString(anyString(),anyString(),anyString())).thenReturn(s);
		when(gitSomaManager.writeFile(anyString(),anyString(),anyString(),anyString(),anyString())).thenThrow(new RuntimeException());
		snykCiIntegrationManager.executeOnboardRepoTask();

	}

	@Test
	public void importRecentlyAddedNewReposTest() throws Exception {
		List<String> gitOrgNames = Arrays.asList("ajanta","pace","victor247");
		Mockito.when(astSnykCIGit.getAllGitOrgs(anyString())).thenReturn(gitOrgNames);
		ObjectMapper mapper = Util.getObjectMapper();
		JsonNode node1 = mapper.readTree("{\"orgs\":[{\"name\":\"GITSOMA/org1\",\"id\" : \"675876869\" },{\"name\":\"GITSOMA/ajanta\",\"id\" : \"5675769869\"},{\"name\":\"pace\",\"id\" : \"243546576\"}]}");
		ArrayNode orgs = (ArrayNode) node1.get("orgs");

		Mockito.when(astSnyk.getOrgs(anyString())).thenReturn(orgs);
		Mockito.when(astSnyk.getSnykOrgIntData(anyString())).thenReturn("76r6tuygygytfytgt98yb98u");
		snykCiIntegrationManager.importRecentlyAddedNewRepos("ivp-ast-psyk", 15);
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

}
