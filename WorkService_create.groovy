/* groovylint-disable LineLength */
/**
* @author Ventyx 2014

* Pre-Modify:
* Apply the costing solution for Work to obtain a default Account Code
*/

import com.mincom.ellipse.hook.hooks.ServiceHook
import com.mincom.enterpriseservice.ellipse.dependant.dto.WorkDTO
import com.mincom.enterpriseservice.ellipse.work.WorkServiceCreateRequestDTO
import com.mincom.enterpriseservice.ellipse.standardjob.StandardJobServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.project.ProjectServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.equipment.EquipmentServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.workgroup.WorkGroupServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.ErrorMessageDTO
import com.mincom.enterpriseservice.exception.*

public class WorkService_create extends ServiceHook {

/*
* IMPORTANT!
* Update this Version number EVERY push to GIT
*/

private String version = "4"

private static final Integer ORG_UNIT_LEN = 3
private static final Integer PROD_OP_ACT_LEN = 7

@Override
public Object onPreExecute(Object dto) {

   log.info("WorkService_modify onPreExecute - version: ${version}")

   WorkServiceModifyRequestDTO request = dto

   //String districtCode = request.getDistrictCode()?request.getDistrictCode().trim():""

   WorkDTO work = request.getWork()

   String copyWork = request.getCopyWork()?request.getCopyWork().toString().trim():""
   String districtCode = request.getDistrictCode()?request.getDistrictCode().trim():""
   String standardJob = request.getStdJobNo()?request.getStdJobNo().trim():""
   String projectNo = request.getProjectNo()?request.getProjectNo().trim():""
   String accountCode = request.getAccountCode()?request.getAccountCode().trim():""
   String workGroup = request.getWorkGroup()?request.getWorkGroup().trim():""
   String equipmentRef = request.getEquipmentRef()?request.getEquipmentRef().trim():""
   String equipmentNo = request.getEquipmentNo()?request.getEquipmentNo().trim():""

   // Use Equipment Number if Equipment Reference is not supplied
   if (!equipmentRef) {
      equipmentRef = equipmentNo
   }
   //flags
   boolean isStandardJob = false
   boolean isProject = false
   boolean isCapital = false
   boolean isEquipment = false
   boolean isWorkGroup = false
   String sjEquipmentRef = ""
   String sjWorkGroup = ""
   String sjProjectNo = ""
   String sjAccountCode = ""
   String projectAccount = ""
   String equipmentAccount = ""
   String workGroupAccount = ""
   String costAccount = ""

   // If copying a Work, then have to bypass the rest of the processing
   if (copyWork) {
      log.info("Copying work ${copyWork}")
      return null
   }
   // If Standard Job entered, get default values from Standard Job
   if (standardJob) {
      log.info("Standard Job entered")
      // ini method darimana ?
      // StandardJobServiceReadReplyDTO ?
      (isStandardJob, sjEquipmentRef, sjWorkGroup, sjProjectNo, sjAccountCode) =
         getStandardJobDetails(districtCode, standardJob)

      if (!isStandardJob) {
         //Standard Job is not valid - send to Ellipse for standard error
         return null
      }
      //Fill empty fields using defaults from Standard Job
      equipmentRef = equipmentRef?equipmentRef:sjEquipmentRef
      workGroup = workGroup?workGroup:sjWorkGroup
      projectNo = projectNo?projectNo:sjProjectNo
      accountCode = accountCode?accountCode:sjAccountCode
      log.info("Standard Job defaults applied to empty fields")
   }

   // Equipment Reference is mandatory
   if (equipmentRef.isEmpty()) {
      log.error("Equipment Reference is mandatory")
      //throw EnterpriseServiceOperationException to return to caller
      throw new EnterpriseServiceOperationException(
      new ErrorMessageDTO("", "Equipment Reference is mandatory", "equipmentRef", 0, 0))
   }
   // Work Group is mandatory
   if (workGroup.isEmpty()) {
      log.error("Work Group is mandatory")
      //throw EnterpriseServiceOperationException to return to caller
      throw new EnterpriseServiceOperationException(
      new ErrorMessageDTO("", "Work Group is mandatory", "workGroup", 0, 0))
   }

   log.info("Work ${districtCode}${work.toString()}")

   // Get existing Work costing details, Attributes before modify
   /* groovylint-disable-next-line LineLength */
   (isWork, origEquipmentRef, origWorkGroup, origProjectNo, origAccountCode, actualTotalCost) = getWorkDetails(districtCode, work, useEquipmentNo)
   if (!isWork) {
      //Work is not valid - send to Ellipse for standard error
      return null
   }

   /* Determine Account Code */
   /* PreCheck */
   (isProject, isCapital, projectAccount) = getProjectDetails(districtCode, projectNo)

   if (projectNo)
   {
      log.info("Project Number entered")
      if (!isProject)
      {
         //Project Number is not valid - send to Ellipse for standard error
         return null
         if (isCapital)
         {
         if (projectAccount.isEmpty()) {
            log.error("No Account Code for Capital Project")
            //throw EnterpriseServiceOperationException to return to caller
            throw new EnterpriseServiceOperationException(
            new ErrorMessageDTO("", "No Account Code for Capital Project", "projectNo", 0, 0))
         }
         request.setAccountCode(projectAccount)
       } else {
          // Standard Ellipse Cost logic
          return null
         }
      }

   // Check if Account Code entered
   if (accountCode) {
      log.info("Account Code entered ${accountCode}")
      //Accept entered Account Code
      return null
   }

   (isEquipment, equipmentAccount) = getEquipmentDetails(equipmentRef)

   if (!isEquipment) {
      //Equipment Reference is not valid - send to Ellipse for standard error
      return null
   }
   if (equipmentAccount.length() < ORG_UNIT_LEN + PROD_OP_ACT_LEN) {
      log.error("Equipment Account invalid for deriving WO Account")
      //throw EnterpriseServiceOperationException to return to caller
      throw new EnterpriseServiceOperationException(
      new ErrorMessageDTO("", "Equipment Account invalid for deriving WO Account", "equipmentRef", 0, 0))
   }

   (isWorkGroup, workGroupAccount) = getWorkGroupDetails(workGroup)
   if (!isWorkGroup) {
      //Work Group is not valid - send to Ellipse for standard error
      return null
   }
   if (workGroupAccount.length() < ORG_UNIT_LEN) {
      log.error("Work Group Account invalid for deriving WO Account")
      //throw EnterpriseServiceOperationException to return to caller
      throw new EnterpriseServiceOperationException(
      new ErrorMessageDTO("", "Work Group Account invalid for deriving WO Account", "workGroup", 0, 0))
   }
   //Derive Account Code - chars 1 to 3 of Work Group Account + chars 4 to 10 of Equipment account
   costAccount = workGroupAccount.substring(0, ORG_UNIT_LEN) + equipmentAccount.substring(ORG_UNIT_LEN, ORG_UNIT_LEN + PROD_OP_ACT_LEN)
   //Set Account Code to derived value
   request.setAccountCode(costAccount)
   log.info("Account Code set to derived value ${costAccount}")
   return null
   }

   /**
   * Read the Work to check for modified fields
   * @param District Code, Work, Use Equipment Number
   * @return Project Number, Account Code, Equipment Reference, Work Group
   */
   /* groovylint-disable-next-line NglParseError */
   private def getWorkDetails(String districtCode, WorkDTO work, boolean useEquipmentNo){
      log.info("getWorkDetails ${districtCode}${work}")
      boolean isWork = false
      String equipmentRef = ""
      String workGroup = ""
      String projectNo = ""
      String accountCode = ""
      BigDecimal actualTotalCost = 0
      try {
         WorkServiceReadReplyDTO workReply =
            tools.service.get("Work").read({
               it.districtCode = districtCode
               it.work = work})

         if (useEquipmentNo) {
            equipmentRef = workReply.getEquipmentNo()?workReply.getEquipmentNo().trim():""
         } else {
            equipmentRef = workReply.getEquipmentRef()?workReply.getEquipmentRef().trim():""
         }
         workGroup = workReply.getWorkGroup()?workReply.getWorkGroup().trim():""
         projectNo = workReply.getProjectNo()?workReply.getProjectNo().trim():""
         accountCode = workReply.getAccountCode()?workReply.getAccountCode().trim():""
         actualTotalCost = workReply.actualEquipmentCost + workReply.actualLabCost + workReply.actualMatCost + workReply.actualOtherCost
         isWork = true
      } catch (EnterpriseServiceOperationException e) {
         log.error("Work not valid")
      }
      return [isWork, equipmentRef, workGroup, projectNo, accountCode, actualTotalCost]
   }

   private def getProjectDetails (String districtCode, String projectNo) {
      log.info("getProjectDetails ${projectNo}")
      boolean isProject = false
      boolean isCapital = false
      String accountCode = ""
      try {
         ProjectServiceReadReplyDTO projectReply = tools.service.get("Project").read({
         it.districtCode = districtCode
         it.projectNo = projectNo})
         isCapital = projectReply.getCapitalSw()
         if(isCapital) {
            accountCode = projectReply.getAccountCode()?projectReply.getAccountCode().trim():""
         }
         isProject = true
      } catch (EnterpriseServiceOperationException e) {
         log.error("Project Number not valid")
      }
      return [isProject, isCapital, accountCode]
   }

   private def getEquipmentDetails (String equipmentRef) {
      log.info("getEquipmentDetails ${equipmentRef}")
      boolean isEquipment = false
      String accountCode = ""
      try {
         EquipmentServiceReadReplyDTO equipmentReply = tools.service.get("Equipment").read({
         it.equipmentRef = equipmentRef})
         accountCode = equipmentReply.getAccountCode()?equipmentReply.getAccountCode().trim():""
         isEquipment = true
      } catch (EnterpriseServiceOperationException e) {
         log.error("Equipment Reference not valid")
      }
      return [isEquipment, accountCode]
   }

   /**
   * Read the Work Group and return details
   * @param Work Group
   * @return Is Work Group, Account Code
   */
   private def getWorkGroupDetails (String workGroup) {
      log.info("getWorkGroupDetails ${workGroup}")
      boolean isWorkGroup = false
      String accountCode = ""
      try {
         WorkGroupServiceReadReplyDTO workGroupReply = tools.service.get("WorkGroup").read({
         it.workGroup = workGroup})
         accountCode = workGroupReply.getAccountCode()?workGroupReply.getAccountCode().trim():""
         isWorkGroup = true
      } catch (EnterpriseServiceOperationException e) {
         log.error("Work Group not valid")
      }
      return [isWorkGroup, accountCode]
   }

}
