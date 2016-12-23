package kz.greetgo.dt.dt_manager_compare_performance_test

import java.util

import kz.greetgo.dt.gen._
import scala.collection.JavaConversions._

/**
  * Created by den on 21.12.16.
  */
object TestData {

  private def obj(entries: (String, AstType)*): AstObj = {
    AstObj(new util.HashMap[String, AstType](Map(entries: _*)))
  }

  private def objs(entries: (String, AstType)*): AstArr = {
    AstArr(AstObj(new util.HashMap[String, AstType](Map(entries: _*))))
  }

  val structure: AstObj =
    obj(
      "in" -> obj(
        "accountIndex" -> AstNum()
      ),
      "client" -> obj(
        "account" -> objs(
          "id" -> AstStr(),
          "nextStmtDate" -> AstDat(),
          "hasAdvancedClearing" -> AstBool(),
          "batchDate" -> AstDat(),
          "appliedAdvanceRepaymentDate" -> AstDat(),
          "advanceRepaymentAmount" -> AstNum(),
          "sumOverpaid" -> AstNum(),
          "createTime" -> AstDat(),
          "currentTerm" -> AstNum(),
          "totalRemPrincipalAmt" -> AstNum(),
          "totalRemServiceFee" -> AstNum(),
          "totalRemInterest" -> AstNum(),

          "totalRemLifeInsuranceFee" -> AstNum(),
          "totalRemInstallmentFee" -> AstNum(),
          "totalRemPrepayPkgFee" -> AstNum(),
          "totalRemReplaceServiceFee" -> AstNum(),
          "totalRemAgentFee" -> AstNum(),

          "product" -> obj(
            "advanceRepaymentApply" -> obj(
              "applyBefore" -> AstNum(),
              "canToApply" -> AstBool(),
              "deductBefore" -> AstNum()
            ),
            "ChargeAdvanceRepayment" -> obj(
              "Interest" -> AstStr(),
              "Principal" -> AstStr(),
              "ServiceFee" -> AstStr(),
              "Value_addedServiceFee" -> AstStr()
            ),
            "AdvanceRepaymentServiceFee" -> objs(
              "parameter" -> AstNum()
            )
          ),
          "payPlan" -> objs(
            "remainingInterest" -> AstNum(),
            "remainingPrincipal" -> AstNum(),
            "remainingServiceFee" -> AstNum(),
            "remainingLifeInsurance" -> AstNum(),
            "remainingInstallmentFee" -> AstNum(),
            "remainingPrepayPkgFee" -> AstNum(),
            "remainingReplaceSvcFee" -> AstNum(),
            "remainingAgentFee" -> AstNum()
          )
        )
      )
    )

  val client: AstObj = obj(
    "client" -> obj(
      "tmp" -> obj(
        "interestRate" -> AstNum(),
        "contraExpireDate" -> AstDat(),
        "loanAmt" -> AstNum(),
        "bizDate" -> AstDat()
      ),
      "country" -> AstStr(),
      "account" -> objs(
        "id" -> AstStr(),
        "asd" -> AstStr(),
        "isFrozen" -> AstBool(),
        "isAbnormal" -> AstBool(),
        "title" -> AstStr(),
        "cpdBeginDate" -> AstDat(),
        "transaction" -> objs(
          "id" -> AstNum(),
          "loanUsage" -> AstStr(),
          "type" -> AstStr(),
          "message" -> AstStr(),
          "orderStatus" -> AstStr()
        ),
        "street" -> AstStr(),
        "paidPrincipalAmt" -> AstNum(),
        "paidInterest" -> AstNum(),
        "product" -> obj(
          "ChargeAdvanceRepayment" -> obj("Value_AddedServiceFee" -> AstStr()),
          "LoanMold" -> AstStr(),
          "LoanStatus" -> AstStr()
        )
      )
    ),
    "generateTransaction" -> obj(
      "transactionAmount" -> AstNum()
    ),
    "transId" -> AstNum()
  )

  val client1: AstObj = obj(
    "client1" -> obj(
      "account" -> objs(
        "overdueFines" -> objs(
          "id" -> AstStr(),
          "overdueFine_amount" -> AstNum(),
          "overdueFine_date" -> AstDat(),
          "overdueFine_status" -> AstStr()
        )
      ))
  )

  val person: AstObj = obj(
    "person" -> obj(
      "address" -> objs(
        "street" -> AstStr(),
        "title" -> AstStr()
      )
    )
  )

  val client2: AstObj = obj(
    "client" -> obj(
      "account" -> objs(
        "street" -> AstStr(),
        "transaction" -> objs(
          "type" -> AstStr(),
          "message" -> AstStr()
        )
      ))
  )

  val client3: AstObj = obj(
    "client" -> obj(
      "account" -> objs(
        "asd" -> AstStr(),
        "title" -> AstStr()
      ))
  )

  val client4: AstObj = obj(
    "client" -> obj(
      "account" -> objs(
        "product" -> obj(
          "ChargeAdvanceRepayment" -> obj(
            "Value_AddedServiceFee" -> AstStr()
          )
        )
      ))
  )

  val complexBreak: AstObj = obj(
    "client" -> obj(
      "account" -> objs(
        "closedDate" -> AstDat(),
        "batchDate" -> AstDat()
      )),
    "in" -> obj(
      "accountIndex" -> AstNum(),
      "accountIndexes" -> objs(
        "value" -> AstNum()
      )
    )
  )

  val in: AstObj = obj(
    "accountIndexes" -> objs(
      "value" -> AstNum()
    ),
    "accountIndex" -> AstNum()
  )

}
