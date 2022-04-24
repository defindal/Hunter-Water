/**
* @author Ventyx 2014

* Pre-Modify:
* Apply the costing solution for Work Orders to obtain a default Account Code
*/

import com.mincom.ellipse.hook.hooks.ServiceHook
import com.mincom.enterpriseservice.ellipse.dependant.dto.WorkOrderDTO
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceCreateRequestDTO
import com.mincom.enterpriseservice.ellipse.standardjob.StandardJobServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.project.ProjectServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.equipment.EquipmentServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.workgroup.WorkGroupServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.ErrorMessageDTO
import com.mincom.enterpriseservice.exception.*

public class WorkOrderService_create extends ServiceHook {

/*
* IMPORTANT!
* Update this Version number EVERY push to GIT
*/

private String version = "4"

private static final Integer ORG_UNIT_LEN = 3
private static final Integer PROD_OP_ACT_LEN = 7

@Override
public Object onPreExecute(Object dto) {

   log.info("WorkOrderService_modify onPreExecute - version: ${version}")

   WorkOrderServiceModifyRequestDTO request = dto

   //String districtCode = request.getDistrictCode()?request.getDistrictCode().trim():""

   WorkOrderDTO workOrder = request.getWorkOrder()

   String copyWorkOrder = request.getCopyWorkOrder()?request.getCopyWorkOrder().toString().trim():""
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

   // If copying a Work Order, then have to bypass the rest of the processing
   if (copyWorkOrder) {
      log.info("Copying work order ${copyWorkOrder}")
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

   log.info("Work Order ${districtCode}${workOrder.toString()}")

   // Get existing Work Order costing details, Attributes before modify
   /* groovylint-disable-next-line LineLength */
   (isWorkOrder, origEquipmentRef, origWorkGroup, origProjectNo, origAccountCode, actualTotalCost) = getWorkOrderDetails(districtCode, workOrder, useEquipmentNo)
   if (!isWorkOrder) {
      //Work Order is not valid - send to Ellipse for standard error
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
   * Read the Work Order to check for modified fields
   * @param District Code, Work Order
   * @return Project Number, Account Code, Equipment Reference, Work Group
   */
   /* groovylint-disable-next-line NglParseError */
   private def getWorkOrderDetails(String districtCode, WorkOrderDTO workOrder, boolean useEquipmentNo){
      log.info("getWorkOrderDetails ${districtCode}${workOrder}")
      boolean isWorkOrder = false
      String equipmentRef = ""
      String workGroup = ""
      String projectNo = ""
      String accountCode = ""
      BigDecimal actualTotalCost = 0
      try {
         WorkOrderServiceReadReplyDTO workOrderReply = 
            tools.service.get("WorkOrder").read({
               it.districtCode = districtCode
               it.workOrder = workOrder})

         if (useEquipmentNo) {
            equipmentRef = workOrderReply.getEquipmentNo()?workOrderReply.getEquipmentNo().trim():""
         } else {
            equipmentRef = workOrderReply.getEquipmentRef()?workOrderReply.getEquipmentRef().trim():""
         }
         workGroup = workOrderReply.getWorkGroup()?workOrderReply.getWorkGroup().trim():""
         projectNo = workOrderReply.getProjectNo()?workOrderReply.getProjectNo().trim():""
         accountCode = workOrderReply.getAccountCode()?workOrderReply.getAccountCode().trim():""
         actualTotalCost = workOrderReply.actualEquipmentCost + workOrderReply.actualLabCost + workOrderReply.actualMatCost + workOrderReply.actualOtherCost
         isWorkOrder = true
      } catch (EnterpriseServiceOperationException e) {
      log.error("Work Order not valid")
      }
      return [isWorkOrder, equipmentRef, workGroup, projectNo, accountCode, actualTotalCost]
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
