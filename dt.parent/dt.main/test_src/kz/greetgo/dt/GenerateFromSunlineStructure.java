package kz.greetgo.dt;

import org.testng.annotations.Test;
import scala.math.BigDecimal;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * Created by den on 17.07.16.
 */
public class GenerateFromSunlineStructure {
  @Test
  public void generateStringForAccountDb() {
    String s = "acqId : String\n" +
        "bizDate : Date\n" +
        "inputSource : InputSource\n" +
        "opId : String\n" +
        "org : String\n" +
        "requestTime : String\n" +
        "serviceId : String\n" +
        "serviceSn : String\n" +
        "subTerminalType : String";

    String[] split = s.split("\n");
    int i = 1;
    List<String> dicts = new ArrayList<>();

    for (String s1 : split) {
      String[] split1 = s1.split(" : ");
// id ~ code ~ type ~ parentId ~ dictCode ~ actual ~ removable ~ attrOrder
      String key = "";
      key += "tmp_" + split1[0]; //id
      key += " ~ ";
      key += split1[0];//code
      key += " ~ ";

      String type = "";
      boolean dict = false;
      if (split1[1].equals("BigDecimal") || split1[1].equals("Integer") || split1[1].equals("Long")) type = "NUM";
      if (split1[1].equals("String")) type = "STR";
      if (split1[1].equals("Indicator")) type = "YES_NO";
      if (split1[1].equals("Date")) type = "DATE";
      if (type.equals("")) {
        type = split1[1];
        dicts.add(type);
        dict = true;
      }

      key += dict == true ? "DICT" : type; //type
      key += " ~ ";

      key += "tmp"; //parentId
      key += " ~ ";

      key += dict == true ? type : "(null)"; //dict
      key += " ~ ";

      key += "1"; //actual
      key += " ~ ";

      key += "1"; //removable
      key += " ~ ";

      key += i; //attrOrder

      i++;

      System.out.println(key);
    }

    System.out.println(dicts);


    System.out.println("============");

     /*
    0Gr6W3lJfSksAOIts ~ cn ~ 性别
    0Gr6W3lJfSksAOIts ~ en ~ Gender
 */

    for (String s1 : split) {
      String[] split1 = s1.split(" : ");
      System.out.println("tmp_" + split1[0] + " ~ cn ~ " + split1[0]);
      System.out.println("tmp_" + split1[0] + " ~ en ~ " + split1[0]);
    }

//[AccountType, CollectionReason, InputSource, DdIndicator, DualBillingInd, Gender, LoanMold, SmsInd, AddressType, StmtMediaType]


  }

  /*
          AddressType ~ C ~ code ~ all ~ C ~ 1
        AddressType ~ C ~ title ~ cn ~ 公司地址 ~ 1
        AddressType ~ C ~ title ~ en ~ C ~ 1
   */
  @Test
  public void generateForDicts() {
    String dictName = "AuthTransTerminal";
    String dict = "T00|其他\",\n" +
        "OTC|柜面\",\n" +
        "ATM|ATM\",\n" +
        "POS|POS普通\",\n" +
        "PHT|POSMOTO手工\",\n" +
        "PHE|POSMOTO电子\",\n" +
        "CS|客服\",\n" +
        "IVR|IVR\",\n" +
        "EB|EBANK网银\",\n" +
        "MB|EBANK手机银行\",\n" +
        "HOST|内管\",\n" +
        "THIRD|第三方支付\",\n" +
        "APP|手机客户端\"";

    String[] split = dict.split("\n");
    for (String s : split) {
      String[] split1 = s.split("\\|");
      System.out.println(dictName + " ~ " + split1[0] + " ~ 1");
    }


    System.out.println("====================");
    /*
            AddressType ~ C ~ code ~ all ~ C ~ 1
        AddressType ~ C ~ title ~ cn ~ 公司地址 ~ 1
        AddressType ~ C ~ title ~ en ~ C ~ 1
     */
    for (String s : split) {
      String[] split1 = s.split("\\|");
      System.out.println(dictName + " ~ " + split1[0] + " ~ code ~ all ~ "+ split1[0] +" ~ 1");
      System.out.println(dictName + " ~ " + split1[0] + " ~ title ~ cn ~ "+ split1[1].replace("\"","").replace(",","") +" ~ 1");
      System.out.println(dictName + " ~ " + split1[0] + " ~ title ~ en ~ "+ split1[0] +" ~ 1");
    }


  }

}
