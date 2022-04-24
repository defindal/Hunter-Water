/* groovylint-disable LineLength */
/**
* @author Ventyx 2014
*
* Pre-Modify:
* Apply the costing solution for Work Orders to obtain a default Account Code
*/

import com.mincom.ellipse.hook.hooks.ServiceHook
import com.mincom.enterpriseservice.ellipse.dependant.dto.WorkOrderDTO
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceModifyRequestDTO
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.context.ContextServiceFetchContextReplyDTO
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.project.ProjectServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.equipment.EquipmentServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.workgroup.WorkGroupServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.ErrorMessageDTO
import com.mincom.enterpriseservice.exception.*

public class WorkOrderService_modify extends ServiceHook {

/*
* IMPORTANT!
* Update this Version number EVERY push to GIT
*/

private String version = "4"

private static final String MSF010_TABLE_TYPE_XX = "XX"
private static final String MSF010_TABLE_TYPE_WKR = "+WKR"
private static final Integer ORG_UNIT_LEN = 3
private static final Integer PROD_OP_ACT_LEN = 7

@Override
public Object onPreExecute(Object dto) {

   log.info("WorkOrderService_modify onPreExecute - version: ${version}")

   WorkOrderServiceModifyRequestDTO request = dto

   String districtCode = request.getDistrictCode()?request.getDistrictCode().trim():""

   WorkOrderDTO workOrder = request.getWorkOrder()

   boolean useEquipmentNo = false
   String projectNo = request.getProjectNo()?.trim()
   String accountCode = request.getAccountCode()?.trim()
   String workGroup = request.getWorkGroup()?.trim()
   String equipmentRef = request.getEquipmentRef()?.trim()
   String equipmentNo = request.getEquipmentNo()?.trim()

   // Use Equipment Number if Equipment Reference is not supplied
   if (equipmentRef == null) {
      equipmentRef = equipmentNo
      useEquipmentNo = true
   }
   String origEquipmentRef = ""
   String origWorkGroup = ""
   String origProjectNo = ""
   String origAccountCode = ""
   BigDecimal actualTotalCost = 0
   boolean isContext = false
   boolean isCostExist = false
   boolean isAuthorised = true
   boolean isWorkOrder = false
   boolean isProject = false
   boolean isCapital = false
   boolean isEquipment = false
   boolean isWorkGroup = false
   String employeeId = ""
   String projectAccount = ""
   String equipmentAccount = ""
   String workGroupAccount = ""

   log.info("Work Order ${districtCode}${workOrder.toString()}")

   // Get existing Work Order costing details, Attributes before modify
   /* groovylint-disable-next-line LineLength */
   (isWorkOrder, origEquipmentRef, origWorkGroup, origProjectNo, origAccountCode, actualTotalCost) = getWorkOrderDetails(districtCode, workOrder, useEquipmentNo)
   if (!isWorkOrder) {
      //Work Order is not valid - send to Ellipse for standard error
      return null
   }
   // Default any attributes not sent in request
   if (projectNo == null) {projectNo = origProjectNo}
   if (accountCode == null) {accountCode = origAccountCode}
   if (workGroup == null) {workGroup = origWorkGroup}
   if (equipmentRef == null) {equipmentRef = origEquipmentRef}

   /* Mandate Equipment and Work Groupb*/

   / Equipment Reference is mandatory
   if (equipmentRef.isEmpty()) {
      log.error("Equipment Reference is mandatory")
      /* to return to callerm, throw EnterpriseServiceOperationException */
      throw new EnterpriseServiceOperationException(
         new ErrorMessageDTO("", "Equipment Reference is mandatory", "equipmentRef", 0, 0)
      )
   }

   // Work Group is mandatory
   if (workGroup.isEmpty()) {
      log.error("Work Group is mandatory")
      //throw EnterpriseServiceOperationException to return to caller
      throw new EnterpriseServiceOperationException(
      new ErrorMessageDTO("", "Work Group is mandatory", "workGroup", 0, 0))
   }
   // Check if Work Order costing details will change , Otherwise skip all process
   if (equipmentRef.equals(origEquipmentRef) &&
      workGroup.equals(origWorkGroup) &&
      projectNo.equals(origProjectNo) &&
      accountCode.equals(origAccountCode)) {
         log.info("No costing details changed")
         return null
      }

   /* Determine Account Code */
   /* PreCheck */
   (isContext, employeeId) = getContextDetails(districtCode)
   isCostExist = costExist()
   isAuthorised = getEmployeeAuthority(employeeId))
   (isProject, isCapital, projectAccount) = getProjectDetails(districtCode, projectNo)

   /* is Project Changed? */

   if ( !projectNo.equals(origProjectNo) )
   {
      if (isCostExist)
      {
         if (isAuthorised)
         {
            if (!projectNo.isEmpty()) {
               if (isCapital) {
                  if (!isProject) {
                     //Project Number is not valid - send to Ellipse for standard error
                     return null
                  }
                  if (projectAccount.isEmpty()) {
                     log.error("No Account Code for Capital Project")
                     //throw EnterpriseServiceOperationException to return to caller
                     throw new EnterpriseServiceOperationException(
                     new ErrorMessageDTO("", "No Account Code for Capital Project", "projectNo", 0, 0))
                  }
                  request.setAccountCode(projectAccount)
                  return null
               }
               else {
                  /***************** Standard Ellipse Cost Option Logic ---- getStandardEllipseCodee ****************/
                  return null
               }
            }
         }
         else /* User is not authorised to modify account code */
            throw new EnterpriseServiceOperationException(
            new ErrorMessageDTO("", "Costs exist, not authorised to modify Account Code", "accountCode", 0, 0))
      }
      else /* Cost does not exist */
      {
         if (!projectNo.isEmpty()) {
            if (isCapital)
            {
                  if (!isProject) {
                     //Project Number is not valid - send to Ellipse for standard error
                     return null
                  }
                  if (projectAccount.isEmpty()) {
                     log.error("No Account Code for Capital Project")
                     //throw EnterpriseServiceOperationException to return to caller
                     throw new EnterpriseServiceOperationException(
                     new ErrorMessageDTO("", "No Account Code for Capital Project", "projectNo", 0, 0))
                  }
                  request.setAccountCode(projectAccount)
                  return null
            }
            else {
                  /***************** Standard Ellipse Cost Option Logic ---- getStandardEllipseCodee ****************/
                  return null
            }
         }
      }
   }

/* Project not changed or cleared

   if (!accountCode.equals(origAccountCode))
   {
      if (isCostExist)
         {
            if (isAuthorised)
            {
               if (!accountCode.isEmpty()) {
                  if (isValidAccount) {
                     log.info("Account Code entered ${accountCode}")
                     //Accept entered Account Code
                     return null
                  }
                  else {
                     throw new EnterpriseServiceOperationException(
                     new ErrorMessageDTO("", "Invalid Account Code", "accountCode", 0, 0))
                  }
               }
            }
            else /* User is not authorised to modify account code */
               throw new EnterpriseServiceOperationException(
               new ErrorMessageDTO("", "Costs exist, not authorised to modify Account Code", "accountCode", 0, 0))
         }
         else /* Cost does not exist */
         {
            if (!accountCode.isEmpty()) {
               if (isValidAccount) {
                  log.info("Account Code entered ${accountCode}")
                  //Accept entered Account Code
                  return null
               }
               else {
                  throw new EnterpriseServiceOperationException(
                  new ErrorMessageDTO("", "Invalid Account Code", "accountCode", 0, 0))
               }
            }
         }
   }

/* Account code not changed or cleared

   if (!equipmentRef.equals(origEquipmentRef) ||
      !workGroup.equals(origWorkGroup)
      )
   {
      if (isCostExist)
         {
            if (isAuthorised)
            {
               /* Derive Account Code */
               (isEquipment, equipmentAccount) = getEquipmentDetails(equipmentRef)

               if (equipmentAccount.length() < ORG_UNIT_LEN + PROD_OP_ACT_LEN) {
                  log.error("Equipment Account invalid for deriving WO Account")
                  //throw EnterpriseServiceOperationException to return to caller
                  throw new EnterpriseServiceOperationException(
                  new ErrorMessageDTO("", "Equipment Account invalid for deriving WO Account", "equipmentRef", 0, 0))
               }

               (isWorkGroup, workGroupAccount) = getWorkGroupDetails(workGroup)

               if (workGroupAccount.length() < ORG_UNIT_LEN) {
                  log.error("Work Group Account invalid for deriving WO Account")
                  //throw EnterpriseServiceOperationException to return to caller
                  throw new EnterpriseServiceOperationException(
                  new ErrorMessageDTO("", "Work Group Account invalid for deriving WO Account", "workGroup", 0, 0))
               }
               //Derive Account Code - chars 1 to 3 of Work Group Account + chars 4 to 10 of Equipment account
               costAccount = workGroupAccount.substring(0, ORG_UNIT_LEN) + equipmentAccount.substring(ORG_UNIT_LEN, ORG_UNIT_LEN + PROD_OP_ACT_LEN)

               if (!accountCode.equals(costAccount) && !costAccount.isEmpty()) {
                  request.setAccountCode(costAccount)
                  log.info("Account Code set to derived value ${costAccount}")
                  /************** Throw warning *************/
                  return null
               }
               else (accountCode.equals(costAccount) && !costAccount.isEmpty()) {
                  request.setAccountCode(costAccount)
                  log.info("Account Code set to derived value ${costAccount}")
                  return null
               }
               else {
                  throw new EnterpriseServiceOperationException(
                  new ErrorMessageDTO("", "derived Account Code is cleared", "accountCode", 0, 0))
               }
            } /* User is not authorised to modify account code */
            else
               throw new EnterpriseServiceOperationException(
               new ErrorMessageDTO("", "Costs exist, not authorised to modify Account Code", "accountCode", 0, 0))
         }
         else  /* Cost does not exist */
         {
            /* Derive Account Code */
            (isEquipment, equipmentAccount) = getEquipmentDetails(equipmentRef)

            if (equipmentAccount.length() < ORG_UNIT_LEN + PROD_OP_ACT_LEN) {
               log.error("Equipment Account invalid for deriving WO Account")
               //throw EnterpriseServiceOperationException to return to caller
               throw new EnterpriseServiceOperationException(
               new ErrorMessageDTO("", "Equipment Account invalid for deriving WO Account", "equipmentRef", 0, 0))
            }

            (isWorkGroup, workGroupAccount) = getWorkGroupDetails(workGroup)

            if (workGroupAccount.length() < ORG_UNIT_LEN) {
               log.error("Work Group Account invalid for deriving WO Account")
               //throw EnterpriseServiceOperationException to return to caller
               throw new EnterpriseServiceOperationException(
               new ErrorMessageDTO("", "Work Group Account invalid for deriving WO Account", "workGroup", 0, 0))
            }
            //Derive Account Code - chars 1 to 3 of Work Group Account + chars 4 to 10 of Equipment account
            costAccount = workGroupAccount.substring(0, ORG_UNIT_LEN) + equipmentAccount.substring(ORG_UNIT_LEN, ORG_UNIT_LEN + PROD_OP_ACT_LEN)

            if (!accountCode.equals(costAccount) && !costAccount.isEmpty()) {
               request.setAccountCode(costAccount)
               log.info("Account Code set to derived value ${costAccount}")
               /************** Throw warning *************/
               return null
            }
            else (accountCode.equals(costAccount) && !costAccount.isEmpty()) {
               request.setAccountCode(costAccount)
               log.info("Account Code set to derived value ${costAccount}")
               return null
            }
            else {
               throw new EnterpriseServiceOperationException(
               new ErrorMessageDTO("", "derived Account Code is cleared", "accountCode", 0, 0))
            }
         }
      if (accountCode.isEmpty())
      {
         throw new EnterpriseServiceOperationException(
         new ErrorMessageDTO("", "Invalid Account Code/Empty", "accountCode", 0, 0))
      }
   }

   /**
   * Read the Work Order to check for modified fields
   * @param District Code, Work Order
   * @return Project Number, Account Code, Equipment Reference, Work Group
   */
   private def getWorkOrderDetails (String districtCode, WorkOrderDTO workOrder, boolean useEquipmentNo) {
      log.info("getWorkOrderDetails ${districtCode}${workOrder}")
      boolean isWorkOrder = false
      String equipmentRef = ""
      String workGroup = ""
      String projectNo = ""
      String accountCode = ""
      BigDecimal actualTotalCost = 0
      try {
         WorkOrderServiceReadReplyDTO workOrderReply = tools.service.get("WorkOrder").read({
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

   /**
   * Fetch Context data to get Employee Id for logged-in user
   * @param District Code
   * @return Context Found, Employee Id
   */
   private def getContextDetails (String districtCode) {
      log.info("getContextDetails for logged-in user")
      boolean isContext = false
      String employeeId = ""
      try {
         ContextServiceFetchContextReplyDTO contextReply = tools.service.get("Context").fetchContext({
         it.district = districtCode})
         employeeId = contextReply.getEmployeeId()
         isContext = true
      } catch (EnterpriseServiceOperationException e) {
      log.info("Cannot get Context to determine Employee Id")
      }
      return [isContext, employeeId]
   }

   /**
   * Read the +WKR Table to check if Employee is authorised
   * @param Employee Id
   * @return Employee Authorised
   */
   private def getEmployeeAuthority (String employeeId) {
      log.info("getEmployeeAuthority ${employeeId}")
      boolean isAuthorised = true
      try {
         TableServiceReadReplyDTO tableReply = tools.service.get("Table").read({
         it.tableType = MSF010_TABLE_TYPE_XX
         it.tableCode = MSF010_TABLE_TYPE_WKR})
         isAuthorised = false
         TableServiceReadReplyDTO codeReply = tools.service.get("Table").read({
         it.tableType = MSF010_TABLE_TYPE_WKR
         it.tableCode = employeeId})
         isAuthorised = true
      } catch (EnterpriseServiceOperationException e) {
         log.error("Employee not authorised to change costing")
      }
      return isAuthorised
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
