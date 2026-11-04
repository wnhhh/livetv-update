package top.yogiczy.mytv.core.data.utils

/**
 * 频道地区识别
 *
 * 只根据频道名称做匹配，用于在展示层把「地方频道」「国际频道」这类大分组
 * 再细分为省级行政区或国家，不修改频道和分组数据本身。
 */
object ChannelRegion {

    /**
     * 省级行政区关键词
     *
     * 只收录两个汉字以上的地名与台名，避免单字简称（如「青」）把「青岛」误判到青海。
     * 香港、澳门、台湾属于中国，放在本表内，不进入国家表。
     */
    private val chinaKeywords: List<Pair<String, String>> = listOf(
        "北京" to "北京", "京视" to "北京",
        "上海" to "上海", "东方卫视" to "上海", "五星体育" to "上海", "哈哈炫动" to "上海",
        "天津" to "天津",
        "重庆" to "重庆",
        "河北" to "河北", "石家庄" to "河北", "唐山" to "河北", "保定" to "河北", "邯郸" to "河北",
        "山西" to "山西", "太原" to "山西",
        "内蒙古" to "内蒙古", "内蒙" to "内蒙古", "呼和浩特" to "内蒙古", "包头" to "内蒙古",
        "辽宁" to "辽宁", "沈阳" to "辽宁", "大连" to "辽宁", "鞍山" to "辽宁",
        "吉林" to "吉林", "长春" to "吉林",
        "黑龙江" to "黑龙江", "哈尔滨" to "黑龙江", "齐齐哈尔" to "黑龙江",
        "江苏" to "江苏", "南京" to "江苏", "苏州" to "江苏", "无锡" to "江苏", "徐州" to "江苏",
        "常州" to "江苏", "扬州" to "江苏", "南通" to "江苏", "盐城" to "江苏",
        "浙江" to "浙江", "杭州" to "浙江", "宁波" to "浙江", "温州" to "浙江", "绍兴" to "浙江",
        "金华" to "浙江", "嘉兴" to "浙江", "湖州" to "浙江",
        "安徽" to "安徽", "合肥" to "安徽", "芜湖" to "安徽",
        "福建" to "福建", "福州" to "福建", "厦门" to "福建", "泉州" to "福建", "漳州" to "福建",
        "江西" to "江西", "南昌" to "江西", "赣州" to "江西",
        "山东" to "山东", "济南" to "山东", "青岛" to "山东", "烟台" to "山东", "潍坊" to "山东",
        "淄博" to "山东", "临沂" to "山东",
        "河南" to "河南", "郑州" to "河南", "洛阳" to "河南", "南阳" to "河南",
        "湖北" to "湖北", "武汉" to "湖北", "宜昌" to "湖北", "襄阳" to "湖北",
        "湖南" to "湖南", "长沙" to "湖南", "株洲" to "湖南", "湘潭" to "湖南",
        "广东" to "广东", "广州" to "广东", "深圳" to "广东", "珠海" to "广东", "佛山" to "广东",
        "东莞" to "广东", "中山" to "广东", "汕头" to "广东", "惠州" to "广东", "湛江" to "广东",
        "江门" to "广东",
        "广西" to "广西", "南宁" to "广西", "桂林" to "广西", "柳州" to "广西",
        "海南" to "海南", "海口" to "海南", "三亚" to "海南",
        "四川" to "四川", "成都" to "四川", "绵阳" to "四川", "宜宾" to "四川",
        "贵州" to "贵州", "贵阳" to "贵州", "遵义" to "贵州",
        "云南" to "云南", "昆明" to "云南", "大理" to "云南", "丽江" to "云南",
        "西藏" to "西藏", "拉萨" to "西藏",
        "陕西" to "陕西", "西安" to "陕西", "宝鸡" to "陕西", "咸阳" to "陕西",
        "甘肃" to "甘肃", "兰州" to "甘肃", "天水" to "甘肃",
        "青海" to "青海", "西宁" to "青海",
        "宁夏" to "宁夏", "银川" to "宁夏",
        "新疆" to "新疆", "乌鲁木齐" to "新疆", "喀什" to "新疆",
        "香港" to "香港", "翡翠台" to "香港", "明珠台" to "香港", "TVB" to "香港",
        "凤凰卫视" to "香港", "ViuTV" to "香港", "开电视" to "香港",
        "澳门" to "澳门", "澳视" to "澳门", "澳门莲花" to "澳门",
        "台湾" to "台湾", "台北" to "台湾", "中天" to "台湾", "东森" to "台湾", "三立" to "台湾",
        "民视" to "台湾", "台视" to "台湾", "华视" to "台湾", "中视" to "台湾", "八大" to "台湾",
        "纬来" to "台湾", "非凡" to "台湾",
    )

    /**
     * 国家关键词
     *
     * 英文缩写只收录不易歧义的（NG 之类短词不收）。
     */
    private val countryKeywords: List<Pair<String, String>> = listOf(
        "美国" to "美国", "USA" to "美国", "CNN" to "美国", "FOX" to "美国", "NBC" to "美国",
        "HBO" to "美国", "Discovery" to "美国", "国家地理" to "美国",
        "英国" to "英国", "BBC" to "英国", "ITV" to "英国", "Sky News" to "英国",
        "日本" to "日本", "NHK" to "日本", "富士" to "日本", "东京" to "日本",
        "韩国" to "韩国", "KBS" to "韩国", "MBC" to "韩国", "首尔" to "韩国",
        "俄罗斯" to "俄罗斯", "RT " to "俄罗斯", "莫斯科" to "俄罗斯",
        "法国" to "法国", "France" to "法国", "TV5" to "法国", "巴黎" to "法国",
        "德国" to "德国", "ZDF" to "德国", "ARD" to "德国", "DW" to "德国",
        "意大利" to "意大利", "RAI" to "意大利",
        "西班牙" to "西班牙", "RTVE" to "西班牙",
        "葡萄牙" to "葡萄牙", "RTP" to "葡萄牙",
        "荷兰" to "荷兰", "NPO" to "荷兰",
        "比利时" to "比利时", "VRT" to "比利时",
        "瑞士" to "瑞士", "SRF" to "瑞士",
        "奥地利" to "奥地利", "ORF" to "奥地利",
        "瑞典" to "瑞典", "SVT" to "瑞典",
        "挪威" to "挪威", "NRK" to "挪威",
        "丹麦" to "丹麦", "芬兰" to "芬兰", "波兰" to "波兰",
        "乌克兰" to "乌克兰", "希腊" to "希腊", "土耳其" to "土耳其", "TRT" to "土耳其",
        "以色列" to "以色列", "伊朗" to "伊朗",
        "沙特" to "沙特阿拉伯", "阿拉伯" to "沙特阿拉伯",
        "阿联酋" to "阿联酋", "迪拜" to "阿联酋",
        "印度" to "印度", "India" to "印度",
        "巴基斯坦" to "巴基斯坦", "孟加拉" to "孟加拉", "斯里兰卡" to "斯里兰卡",
        "尼泊尔" to "尼泊尔",
        "泰国" to "泰国", "Thai" to "泰国", "曼谷" to "泰国",
        "越南" to "越南", "VTV" to "越南",
        "老挝" to "老挝", "柬埔寨" to "柬埔寨", "缅甸" to "缅甸",
        "马来西亚" to "马来西亚", "吉隆坡" to "马来西亚",
        "新加坡" to "新加坡", "Singapore" to "新加坡",
        "印度尼西亚" to "印度尼西亚", "印尼" to "印度尼西亚",
        "菲律宾" to "菲律宾", "Philippines" to "菲律宾",
        "蒙古" to "蒙古", "哈萨克" to "哈萨克斯坦",
        "澳大利亚" to "澳大利亚", "澳洲" to "澳大利亚",
        "新西兰" to "新西兰",
        "加拿大" to "加拿大", "Canada" to "加拿大", "CBC" to "加拿大",
        "墨西哥" to "墨西哥",
        "巴西" to "巴西", "Brazil" to "巴西",
        "阿根廷" to "阿根廷", "智利" to "智利", "哥伦比亚" to "哥伦比亚",
        "埃及" to "埃及", "南非" to "南非", "尼日利亚" to "尼日利亚", "肯尼亚" to "肯尼亚",
    )

    // 长词优先，保证「青岛」不会被「青海」以外的前缀误吃掉
    private val orderedChina = chinaKeywords.sortedByDescending { it.first.length }
    private val orderedCountry = countryKeywords.sortedByDescending { it.first.length }

    /**
     * 识别频道所属地区，返回分组名；无法识别返回 null
     */
    fun of(channelName: String): String? {
        if (channelName.isBlank()) return null

        val upperName = channelName.uppercase()

        orderedChina.firstOrNull { (keyword, _) ->
            channelName.contains(keyword) || upperName.contains(keyword.uppercase())
        }?.let { return it.second }

        orderedCountry.firstOrNull { (keyword, _) ->
            channelName.contains(keyword) || upperName.contains(keyword.uppercase())
        }?.let { return it.second }

        return null
    }
}
