package com.yl.ylvoicewakeup.utils;

import java.util.ArrayList;
import java.util.List;

public class XTTSParams {

    public String vcn = "xiaofeng";
    public int language =1 ;
    public int pitch = 50;
    public int speed = 50;
    public int volume = 50;

    /**
     * 测试文本资源，资源顺序与发音人、语种相对应
     * @return
     */
    public static List<String> testTxt() {
        List<String> txtList = new ArrayList<>();
        txtList.add("你好，我叫晓燕，很高兴认识你。");
        txtList.add("你好，我叫晓峰，很高兴认识你。");
//        txtList.add("The weather is good for going out today.");
//        txtList.add("兄はきょうはいい天気だから遊びに行くにはよい");
//        txtList.add("오빠 오늘 날씨가 너무 좋아서 밖에 나가 놀기 좋아요");
//        txtList.add("Il fait beau aujourd’hui pour sortir.");
//        txtList.add("Hermano hace un buen día para salir");
//        txtList.add("Сегодня прекрасный день для прогулок.");
//        txtList.add("Es ist ein schöner tag zum ausgehen.");
        return txtList;
    }

    public static List<ParamInfo> getVCN() {
        List<ParamInfo> vcnList = new ArrayList<>();
        vcnList.add(new ParamInfo("xiaoyan", "xiaoyan(中文)"));
        vcnList.add(new ParamInfo("xiaofeng", "xiaofeng(中文)"));
//        vcnList.add(new ParamInfo("catherine", "catherine(英文)"));
//        vcnList.add(new ParamInfo("zhongcun", "zhongcun（日语）"));
//        vcnList.add(new ParamInfo("kim", "kim（韩语）"));
//        vcnList.add(new ParamInfo("mariane", "mariane（法语）"));
//        vcnList.add(new ParamInfo("felisa", "felisa（西班牙语）"));
//        vcnList.add(new ParamInfo("keshu", "keshu（俄语）"));
//        vcnList.add(new ParamInfo("christiance", "christiance（德语）"));
        return vcnList;
    }

    public static List<ParamInfo> getlanguage() {
        List<ParamInfo> languageList = new ArrayList<>();
        languageList.add(new ParamInfo("1", "中文"));
        languageList.add(new ParamInfo("1", "中文"));
//        languageList.add(new ParamInfo("2", "英文"));
//        languageList.add(new ParamInfo("5", "日语"));
//        languageList.add(new ParamInfo("16", "韩语"));
//        languageList.add(new ParamInfo("3", "法语"));
//        languageList.add(new ParamInfo("23", "西班牙语"));
//        languageList.add(new ParamInfo("6", "俄语"));
//        languageList.add(new ParamInfo("9", "德语"));
        return languageList;
    }

}
