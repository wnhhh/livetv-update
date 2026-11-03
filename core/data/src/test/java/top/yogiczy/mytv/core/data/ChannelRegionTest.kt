package top.yogiczy.mytv.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import top.yogiczy.mytv.core.data.utils.ChannelRegion

class ChannelRegionTest {

    @Test
    fun `省份关键词优先于城市别名`() {
        assertEquals("山东", ChannelRegion.of("青岛新闻综合"))
        assertEquals("青海", ChannelRegion.of("青海卫视"))
        assertEquals("广东", ChannelRegion.of("深圳卫视"))
        assertEquals("江苏", ChannelRegion.of("南京十八频道"))
    }

    @Test
    fun `特殊台名能正确归属`() {
        assertEquals("上海", ChannelRegion.of("东方卫视"))
        assertEquals("香港", ChannelRegion.of("凤凰卫视资讯台"))
        assertEquals("香港", ChannelRegion.of("翡翠台"))
        assertEquals("台湾", ChannelRegion.of("中天新闻"))
        assertEquals("澳门", ChannelRegion.of("澳视澳门"))
    }

    @Test
    fun `国家关键词能识别`() {
        assertEquals("日本", ChannelRegion.of("NHK World"))
        assertEquals("韩国", ChannelRegion.of("KBS1"))
        assertEquals("美国", ChannelRegion.of("CNN International"))
        assertEquals("英国", ChannelRegion.of("BBC News"))
    }

    @Test
    fun `无法识别的频道返回空`() {
        assertNull(ChannelRegion.of("CCTV1"))
        assertNull(ChannelRegion.of("测试频道"))
        // 卫视名本身能识别出省份，但「卫视」分组在侧边栏层受保护、不会被拆
        assertEquals("湖南", ChannelRegion.of("湖南卫视"))
    }

    @Test
    fun `不被短词误判`() {
        // 「华视」属台湾，而「华」不应把「华数」之类误判
        assertEquals("台湾", ChannelRegion.of("华视"))
        assertNull(ChannelRegion.of("华数传媒"))
    }
}
