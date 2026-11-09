package com.example.xregion;

/** 全量国家/地区表：ISO 3166-1 alpha-2 共 249 个 + CLDR 常见特殊地区；旗帜由代码生成，不用图片资源。 */
public final class Countries {

    /** 常用国家/地区在前，其余按 ISO 码排序。 */
    public static final String[][] LIST = {
            {"cn", "中国"}, {"tw", "中国台湾"}, {"hk", "中国香港"}, {"mo", "中国澳门"},
            {"jp", "日本"}, {"kr", "韩国"}, {"us", "美国"}, {"ca", "加拿大"},
            {"mx", "墨西哥"}, {"br", "巴西"}, {"ar", "阿根廷"}, {"gb", "英国"},
            {"ie", "爱尔兰"}, {"fr", "法国"}, {"de", "德国"}, {"it", "意大利"},
            {"es", "西班牙"}, {"pt", "葡萄牙"}, {"nl", "荷兰"}, {"be", "比利时"},
            {"ch", "瑞士"}, {"at", "奥地利"}, {"se", "瑞典"}, {"no", "挪威"},
            {"dk", "丹麦"}, {"fi", "芬兰"}, {"pl", "波兰"}, {"cz", "捷克"},
            {"ru", "俄罗斯"}, {"ua", "乌克兰"}, {"tr", "土耳其"}, {"in", "印度"},
            {"sg", "新加坡"}, {"my", "马来西亚"}, {"th", "泰国"}, {"vn", "越南"},
            {"id", "印度尼西亚"}, {"ph", "菲律宾"}, {"au", "澳大利亚"}, {"nz", "新西兰"},
            {"ae", "阿拉伯联合酋长国"}, {"sa", "沙特阿拉伯"}, {"il", "以色列"}, {"za", "南非"},
            {"eg", "埃及"}, {"ng", "尼日利亚"}, {"ad", "安道尔"}, {"af", "阿富汗"},
            {"ag", "安提瓜和巴布达"}, {"ai", "安圭拉"}, {"al", "阿尔巴尼亚"}, {"am", "亚美尼亚"},
            {"ao", "安哥拉"}, {"aq", "南极洲"}, {"as", "美属萨摩亚"}, {"aw", "阿鲁巴"},
            {"ax", "奥兰群岛"}, {"az", "阿塞拜疆"}, {"ba", "波斯尼亚和黑塞哥维那"}, {"bb", "巴巴多斯"},
            {"bd", "孟加拉国"}, {"bf", "布基纳法索"}, {"bg", "保加利亚"}, {"bh", "巴林"},
            {"bi", "布隆迪"}, {"bj", "贝宁"}, {"bl", "圣巴泰勒米"}, {"bm", "百慕大"},
            {"bn", "文莱"}, {"bo", "玻利维亚"}, {"bq", "荷属加勒比区"}, {"bs", "巴哈马"},
            {"bt", "不丹"}, {"bv", "布韦岛"}, {"bw", "博茨瓦纳"}, {"by", "白俄罗斯"},
            {"bz", "伯利兹"}, {"cc", "科科斯（基林）群岛"}, {"cd", "刚果（金）"}, {"cf", "中非共和国"},
            {"cg", "刚果（布）"}, {"ci", "科特迪瓦"}, {"ck", "库克群岛"}, {"cl", "智利"},
            {"cm", "喀麦隆"}, {"co", "哥伦比亚"}, {"cr", "哥斯达黎加"}, {"cu", "古巴"},
            {"cv", "佛得角"}, {"cw", "库拉索"}, {"cx", "圣诞岛"}, {"cy", "塞浦路斯"},
            {"dj", "吉布提"}, {"dm", "多米尼克"}, {"do", "多米尼加共和国"}, {"dz", "阿尔及利亚"},
            {"ec", "厄瓜多尔"}, {"ee", "爱沙尼亚"}, {"eh", "西撒哈拉"}, {"er", "厄立特里亚"},
            {"et", "埃塞俄比亚"}, {"fj", "斐济"}, {"fk", "福克兰群岛"}, {"fm", "密克罗尼西亚"},
            {"fo", "法罗群岛"}, {"ga", "加蓬"}, {"gd", "格林纳达"}, {"ge", "格鲁吉亚"},
            {"gf", "法属圭亚那"}, {"gg", "根西岛"}, {"gh", "加纳"}, {"gi", "直布罗陀"},
            {"gl", "格陵兰"}, {"gm", "冈比亚"}, {"gn", "几内亚"}, {"gp", "瓜德罗普"},
            {"gq", "赤道几内亚"}, {"gr", "希腊"}, {"gs", "南乔治亚和南桑威奇群岛"}, {"gt", "危地马拉"},
            {"gu", "关岛"}, {"gw", "几内亚比绍"}, {"gy", "圭亚那"}, {"hm", "赫德岛和麦克唐纳群岛"},
            {"hn", "洪都拉斯"}, {"hr", "克罗地亚"}, {"ht", "海地"}, {"hu", "匈牙利"},
            {"im", "马恩岛"}, {"io", "英属印度洋领地"}, {"iq", "伊拉克"}, {"ir", "伊朗"},
            {"is", "冰岛"}, {"je", "泽西岛"}, {"jm", "牙买加"}, {"jo", "约旦"},
            {"ke", "肯尼亚"}, {"kg", "吉尔吉斯斯坦"}, {"kh", "柬埔寨"}, {"ki", "基里巴斯"},
            {"km", "科摩罗"}, {"kn", "圣基茨和尼维斯"}, {"kp", "朝鲜"}, {"kw", "科威特"},
            {"ky", "开曼群岛"}, {"kz", "哈萨克斯坦"}, {"la", "老挝"}, {"lb", "黎巴嫩"},
            {"lc", "圣卢西亚"}, {"li", "列支敦士登"}, {"lk", "斯里兰卡"}, {"lr", "利比里亚"},
            {"ls", "莱索托"}, {"lt", "立陶宛"}, {"lu", "卢森堡"}, {"lv", "拉脱维亚"},
            {"ly", "利比亚"}, {"ma", "摩洛哥"}, {"mc", "摩纳哥"}, {"md", "摩尔多瓦"},
            {"me", "黑山"}, {"mf", "法属圣马丁"}, {"mg", "马达加斯加"}, {"mh", "马绍尔群岛"},
            {"mk", "北马其顿"}, {"ml", "马里"}, {"mm", "缅甸"}, {"mn", "蒙古"},
            {"mp", "北马里亚纳群岛"}, {"mq", "马提尼克"}, {"mr", "毛里塔尼亚"}, {"ms", "蒙特塞拉特"},
            {"mt", "马耳他"}, {"mu", "毛里求斯"}, {"mv", "马尔代夫"}, {"mw", "马拉维"},
            {"mz", "莫桑比克"}, {"na", "纳米比亚"}, {"nc", "新喀里多尼亚"}, {"ne", "尼日尔"},
            {"nf", "诺福克岛"}, {"ni", "尼加拉瓜"}, {"np", "尼泊尔"}, {"nr", "瑙鲁"},
            {"nu", "纽埃"}, {"om", "阿曼"}, {"pa", "巴拿马"}, {"pe", "秘鲁"},
            {"pf", "法属波利尼西亚"}, {"pg", "巴布亚新几内亚"}, {"pk", "巴基斯坦"}, {"pm", "圣皮埃尔和密克隆群岛"},
            {"pn", "皮特凯恩群岛"}, {"pr", "波多黎各"}, {"ps", "巴勒斯坦领土"}, {"pw", "帕劳"},
            {"py", "巴拉圭"}, {"qa", "卡塔尔"}, {"re", "留尼汪"}, {"ro", "罗马尼亚"},
            {"rs", "塞尔维亚"}, {"rw", "卢旺达"}, {"sb", "所罗门群岛"}, {"sc", "塞舌尔"},
            {"sd", "苏丹"}, {"sh", "圣赫勒拿"}, {"si", "斯洛文尼亚"}, {"sj", "斯瓦尔巴和扬马延"},
            {"sk", "斯洛伐克"}, {"sl", "塞拉利昂"}, {"sm", "圣马力诺"}, {"sn", "塞内加尔"},
            {"so", "索马里"}, {"sr", "苏里南"}, {"ss", "南苏丹"}, {"st", "圣多美和普林西比"},
            {"sv", "萨尔瓦多"}, {"sx", "荷属圣马丁"}, {"sy", "叙利亚"}, {"sz", "斯威士兰"},
            {"tc", "特克斯和凯科斯群岛"}, {"td", "乍得"}, {"tf", "法属南部领地"}, {"tg", "多哥"},
            {"tj", "塔吉克斯坦"}, {"tk", "托克劳"}, {"tl", "东帝汶"}, {"tm", "土库曼斯坦"},
            {"tn", "突尼斯"}, {"to", "汤加"}, {"tt", "特立尼达和多巴哥"}, {"tv", "图瓦卢"},
            {"tz", "坦桑尼亚"}, {"ug", "乌干达"}, {"um", "美国本土外小岛屿"}, {"uy", "乌拉圭"},
            {"uz", "乌兹别克斯坦"}, {"va", "梵蒂冈"}, {"vc", "圣文森特和格林纳丁斯"}, {"ve", "委内瑞拉"},
            {"vg", "英属维尔京群岛"}, {"vi", "美属维尔京群岛"}, {"vu", "瓦努阿图"}, {"wf", "瓦利斯和富图纳"},
            {"ws", "萨摩亚"}, {"ye", "也门"}, {"yt", "马约特"}, {"zm", "赞比亚"},
            {"zw", "津巴布韦"},
            // 非 ISO 3166-1 的常见地区代码（CLDR）；EU/UN 等超国家组织与伪代码不收录。
            {"ac", "阿森松岛"}, {"cp", "克利珀顿岛"}, {"cq", "萨克岛"}, {"dg", "迪戈加西亚岛"},
            {"ea", "休达及梅利利亚"}, {"ic", "加纳利群岛"}, {"ta", "特里斯坦-达库尼亚群岛"},
    };

    /** ISO 码与特殊地区对应的主 MCC；无移动网络的国家/地区返回空串。 */
    public static String mcc(String iso) {
        String k = iso == null ? "" : iso.toLowerCase();
        switch (k) {
            case "cn": return "460";
            case "tw": return "466";
            case "hk": return "454";
            case "mo": return "455";
            case "jp": return "440";
            case "kr": return "450";
            case "us": return "310";
            case "ca": return "302";
            case "mx": return "334";
            case "br": return "724";
            case "ar": return "722";
            case "gb": return "234";
            case "ie": return "272";
            case "fr": return "208";
            case "de": return "262";
            case "it": return "222";
            case "es": return "214";
            case "pt": return "268";
            case "nl": return "204";
            case "be": return "206";
            case "ch": return "228";
            case "at": return "232";
            case "se": return "240";
            case "no": return "242";
            case "dk": return "238";
            case "fi": return "244";
            case "pl": return "260";
            case "cz": return "230";
            case "ru": return "250";
            case "ua": return "255";
            case "tr": return "286";
            case "in": return "404";
            case "sg": return "525";
            case "my": return "502";
            case "th": return "520";
            case "vn": return "452";
            case "id": return "510";
            case "ph": return "515";
            case "au": return "505";
            case "nz": return "530";
            case "ae": return "424";
            case "sa": return "420";
            case "il": return "425";
            case "za": return "655";
            case "eg": return "602";
            case "ng": return "621";
            case "ad": return "213";
            case "af": return "412";
            case "ag": return "344";
            case "ai": return "365";
            case "al": return "276";
            case "am": return "283";
            case "ao": return "631";
            case "as": return "311";
            case "aw": return "363";
            case "ax": return "244";
            case "az": return "400";
            case "ba": return "218";
            case "bb": return "342";
            case "bd": return "470";
            case "bf": return "613";
            case "bg": return "284";
            case "bh": return "426";
            case "bi": return "642";
            case "bj": return "616";
            case "bl": return "340";
            case "bm": return "310";
            case "bn": return "528";
            case "bo": return "736";
            case "bq": return "362";
            case "bs": return "364";
            case "bt": return "402";
            case "bw": return "652";
            case "by": return "257";
            case "bz": return "702";
            case "cc": return "505";
            case "cd": return "630";
            case "cf": return "623";
            case "cg": return "629";
            case "ci": return "612";
            case "ck": return "548";
            case "cl": return "730";
            case "cm": return "624";
            case "co": return "732";
            case "cr": return "712";
            case "cu": return "368";
            case "cv": return "625";
            case "cw": return "362";
            case "cx": return "505";
            case "cy": return "280";
            case "dj": return "638";
            case "dm": return "366";
            case "do": return "370";
            case "dz": return "603";
            case "ec": return "740";
            case "ee": return "248";
            case "er": return "657";
            case "et": return "636";
            case "fj": return "542";
            case "fk": return "750";
            case "fm": return "550";
            case "fo": return "288";
            case "ga": return "628";
            case "gd": return "352";
            case "ge": return "282";
            case "gf": return "340";
            case "gg": return "234";
            case "gh": return "620";
            case "gi": return "266";
            case "gl": return "290";
            case "gm": return "607";
            case "gn": return "611";
            case "gp": return "340";
            case "gq": return "627";
            case "gr": return "202";
            case "gt": return "704";
            case "gu": return "310";
            case "gw": return "632";
            case "gy": return "738";
            case "hn": return "708";
            case "hr": return "219";
            case "ht": return "372";
            case "hu": return "216";
            case "im": return "234";
            case "iq": return "418";
            case "ir": return "432";
            case "is": return "274";
            case "je": return "234";
            case "jm": return "338";
            case "jo": return "416";
            case "ke": return "639";
            case "kg": return "437";
            case "kh": return "456";
            case "ki": return "545";
            case "km": return "654";
            case "kn": return "356";
            case "kp": return "467";
            case "kw": return "419";
            case "ky": return "338";
            case "kz": return "401";
            case "la": return "457";
            case "lb": return "415";
            case "lc": return "338";
            case "li": return "295";
            case "lk": return "413";
            case "lr": return "618";
            case "ls": return "651";
            case "lt": return "246";
            case "lu": return "270";
            case "lv": return "247";
            case "ly": return "606";
            case "ma": return "604";
            case "mc": return "212";
            case "md": return "255";
            case "me": return "297";
            case "mf": return "340";
            case "mg": return "646";
            case "mh": return "551";
            case "mk": return "294";
            case "ml": return "610";
            case "mm": return "414";
            case "mn": return "428";
            case "mp": return "310";
            case "mq": return "340";
            case "mr": return "609";
            case "ms": return "338";
            case "mt": return "278";
            case "mu": return "617";
            case "mv": return "472";
            case "mw": return "650";
            case "mz": return "643";
            case "na": return "649";
            case "nc": return "546";
            case "ne": return "614";
            case "nf": return "505";
            case "ni": return "710";
            case "np": return "429";
            case "nr": return "536";
            case "nu": return "555";
            case "om": return "422";
            case "pa": return "714";
            case "pe": return "716";
            case "pf": return "547";
            case "pg": return "537";
            case "pk": return "410";
            case "pm": return "308";
            case "pr": return "310";
            case "ps": return "425";
            case "pw": return "552";
            case "py": return "744";
            case "qa": return "427";
            case "re": return "647";
            case "ro": return "226";
            case "rs": return "220";
            case "rw": return "635";
            case "sb": return "540";
            case "sc": return "633";
            case "sd": return "634";
            case "sh": return "658";
            case "si": return "293";
            case "sj": return "242";
            case "sk": return "231";
            case "sl": return "619";
            case "sm": return "292";
            case "sn": return "608";
            case "so": return "637";
            case "sr": return "746";
            case "ss": return "659";
            case "st": return "626";
            case "sv": return "706";
            case "sx": return "362";
            case "sy": return "417";
            case "sz": return "653";
            case "tc": return "338";
            case "td": return "622";
            case "tg": return "615";
            case "tj": return "436";
            case "tk": return "554";
            case "tl": return "514";
            case "tm": return "438";
            case "tn": return "605";
            case "to": return "539";
            case "tt": return "374";
            case "tv": return "553";
            case "tz": return "640";
            case "ug": return "641";
            case "uy": return "748";
            case "uz": return "434";
            case "va": return "222";
            case "vc": return "360";
            case "ve": return "734";
            case "vg": return "348";
            case "vi": return "310";
            case "vu": return "541";
            case "wf": return "543";
            case "ws": return "549";
            case "ye": return "421";
            case "yt": return "647";
            case "zm": return "645";
            case "zw": return "648";
            case "ac": return "658";
            case "cq": return "234";
            case "ea": return "214";
            case "ic": return "214";
            case "ta": return "658";
            default: return "";
        }
    }

    private Countries() {
    }

    /** 两位字母 -> 国旗 Emoji（U+1F1E6 起两个 regional indicator）。 */
    public static String flag(String iso) {
        if (iso == null || iso.length() != 2) {
            return "\uD83C\uDFF3";
        }
        String u = iso.toUpperCase();
        int a = 0x1F1E6 + (u.charAt(0) - 'A');
        int b = 0x1F1E6 + (u.charAt(1) - 'A');
        return new String(Character.toChars(a)) + new String(Character.toChars(b));
    }

    public static String nameOf(String iso) {
        if (iso == null) {
            return "未知";
        }
        String k = iso.toLowerCase();
        for (String[] row : LIST) {
            if (row[0].equals(k)) {
                return row[1];
            }
        }
        return "未知（" + k + "）";
    }

    public static int indexOf(String iso) {
        if (iso == null) {
            return 0;
        }
        String k = iso.toLowerCase();
        for (int i = 0; i < LIST.length; i++) {
            if (LIST[i][0].equals(k)) {
                return i;
            }
        }
        return 0;
    }

    /** 下拉项文案：旗帜 + 中文名 + (缩写)。 */
    public static String label(String iso) {
        String k = iso == null ? "" : iso.toLowerCase();
        return flag(k) + "  " + nameOf(k) + "  (" + k + ")";
    }

    public static String[] labels() {
        String[] out = new String[LIST.length];
        for (int i = 0; i < LIST.length; i++) {
            out[i] = label(LIST[i][0]);
        }
        return out;
    }
}
