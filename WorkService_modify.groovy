/* groovylint-disable LineLength */
/**
* @author Ventyx 2014
*
* Pre-Modify:
* Apply the costing solution for Work to obtain a default Account Code
*/

import com.mincom.ellipse.hook.hooks.ServiceHook
import com.mincom.enterpriseservice.ellipse.dependant.dto.WorkDTO
import com.mincom.enterpriseservice.ellipse.work.WorkServiceModifyRequestDTO
import com.mincom.enterpriseservice.ellipse.work.WorkServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.context.ContextServiceFetchContextReplyDTO
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.project.ProjectServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.equipment.EquipmentServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.workgroup.WorkGroupServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.ErrorMessageDTO
import com.mincom.enterpriseservice.exception.*

public class WorkService_modify extends ServiceHook {

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

   log.info("WorkService_modify onPreExecute - version: ${version}")

   WorkServiceModifyRequestDTO request = dto

   String districtCode = request.getDistrictCode()?request.getDistrictCode().trim():""

   WorkDTO work = request.getWork()

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
   // flags
   boolean isContext = false
   boolean isCostExist = false
   boolean isAuthorised = true
   boolean isWork = false
   boolean isProject = false
   boolean isCapital = false
   boolean isEquipment = false
   boolean isWorkGroup = false
   String employeeId = ""
   String projectAccount = ""
   String equipmentAccount = ""
   String workGroupAccount = ""

   log.info("Work ${districtCode}${work.toString()}")

   // Get existing Work costing details, Attributes before modify
   /* groovylint-disable-next-line LineLength */
   (isWork, origEquipmentRef, origWorkGroup, origProjectNo, origAccountCode, actualTotalCost) =
      getWorkDetails(districtCode, work, useEquipmentNo)

   if (!isWork) {
      //Work is not valid - send to Ellipse for standard error
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
      /** to return to the caller, throw EnterpriseServiceOperationException */
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
   // Check if Work costing details will change , Otherwise skip all process
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
   // method costExist() couldnt be found
   // isCostExist = costExist()
   isAuthorised = getEmployeeAuthority(employeeId))
   (isProject, isCapital, projectAccount) = getProjectDetails(districtCode, projectNo)

   /* is Project Changed? */

   if (!projectNo.equals(origProjectNo) || projectNo.isEmpty()){
      if (isCostExist){
         if (isAuthorised){
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
               } else {
                  /***************** Standard Ellipse Cost Option Logic ---- getStandardEllipseCodee ****************/
                  return null
               }
            }
         } else { /* User is not authorised to modify account code */
            throw new EnterpriseServiceOperationException(
               new ErrorMessageDTO("", "Costs exist, not authorised to modify Account Code", "accountCode", 0, 0))
         }
      } else { /* Cost does not exist */
         if (!projectNo.isEmpty()) {
            if (isCapital){
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

   /* Project not changed or cleared */
   if (!accountCode.equals(origAccountCode)){
      if (isCostExist){
         if (isAuthorised){
            if (!accountCode.isEmpty()) {
               if (isValidAccount) {
                  log.info("Account Code entered ${accountCode}")
                  //Accept entered Account Code
                  return null
               } else {
                  throw new EnterpriseServiceOperationException(
                     new ErrorMessageDTO("", "Invalid Account Code", "accountCode", 0, 0))
               }
            }
         } else {/* User is not authorised to modify account code */
            throw new EnterpriseServiceOperationException(
               new ErrorMessageDTO("", "Costs exist, not authorised to modify Account Code", "accountCode", 0, 0))
         }
      } else { /* Cost does not exist */
         if (!accountCode.isEmpty()) {
            if (isValidAccount) {
               log.info("Account Code entered ${accountCode}")
               //Accept entered Account Code
               return null
            } else {
               throw new EnterpriseServiceOperationException(
                  new ErrorMessageDTO("", "Invalid Account Code", "accountCode", 0, 0))
            }
         }
      }
   }

   /* Account code not changed or cleared */
   if (!equipmentRef.equals(origEquipmentRef) ||
      !workGroup.equals(origWorkGroup)
      ){
      if (isCostExist) {
         if (isAuthorised) {
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
            } else if (accountCode.equals(costAccount) && !costAccount.isEmpty()) {
               request.setAccountCode(costAccount)
               log.info("Account Code set to derived value ${costAccount}")
               return null
            } else {
               throw new EnterpriseServiceOperationException(
                  new ErrorMessageDTO("", "derived Account Code is cleared", "accountCode", 0, 0))
            }
         } else { /* User is not authorised to modify account code */
            throw new EnterpriseServiceOperationException(
               new ErrorMessageDTO("", "Costs exist, not authorised to modify Account Code", "accountCode", 0, 0))
         }
      } else {  /* Cost does not exist */

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
         } else if (accountCode.equals(costAccount) && !costAccount.isEmpty()) {
            request.setAccountCode(costAccount)
            log.info("Account Code set to derived value ${costAccount}")
            return null
         } else {
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
   * Read the Work to check for modified fields
   * @param District Code, Work , Use Equipment Number
   * @return Project Number, Account Code, Equipment Reference, Work Group
   */
   private def getWorkDetails (String districtCode, WorkDTO work, boolean useEquipmentNo) {
      log.info("getWorkDetails ${districtCode}${work}")
      boolean isWork = false
      String equipmentRef = ""
      String workGroup = ""
      String projectNo = ""
      String accountCode = ""
      BigDecimal actualTotalCost = 0
      try {
         WorkServiceReadReplyDTO workReply = tools.service.get("Work").read({
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
         ContextServiceFetchContextReplyDTO contextReply =
            tools.service.get("Context").fetchContext({
               it.district = districtCode
            })
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
         // ambigous logic
         TableServiceReadReplyDTO tableReply =
            tools.service.get("Table").read({
               it.tableType = MSF010_TABLE_TYPE_XX
               it.tableCode = MSF010_TABLE_TYPE_WKR
            })
         isAuthorised = false
         TableServiceReadReplyDTO codeReply =
            tools.service.get("Table").read({
               it.tableType = MSF010_TABLE_TYPE_WKR
               it.tableCode = employeeId
            })
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
