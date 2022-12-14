package com.md.service.common;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class UserRandomInfo {

    private static String nikeName = "James,William,Lucas,Henry,Jack,Daniel,Michael,Logan,Owen,Ashley,Aaron,Cooper,Alex,Wesley,Adam,Bryson,Jasper,Jason,Cole,Ace,Ivan,Leon,Brandon,Joe,Jenny,Simon,Kylie,Kobe,Jay,Travis,Jared,Jefferey,Hassan,Dash,Mia,Isabella,Emily,Layla,Nora,Lily,Zoe,Stella,Elena,Claire,Alice,Bella,Cora,Eva,Iris,Maria,Lucia,Jasmine,Olive,Blake,Aspen,Myla,Hanna,Julie,Eve";
    private static String headImage = "https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/meta/demo/fulldemoStatic/man1.png,https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/meta/demo/fulldemoStatic/man2.png,https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/meta/demo/fulldemoStatic/man3.png,https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/meta/demo/fulldemoStatic/man4.png,https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/meta/demo/fulldemoStatic/man5.png,https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/meta/demo/fulldemoStatic/man6.png,https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/meta/demo/fulldemoStatic/man7.png,https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/meta/demo/fulldemoStatic/man8.png,https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/meta/demo/fulldemoStatic/man9.png,https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/meta/demo/fulldemoStatic/woman1.png,https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/meta/demo/fulldemoStatic/woman2.png," +
                    "https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/meta/demo/fulldemoStatic/woman3.png,https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/meta/demo/fulldemoStatic/woman4.png,https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/meta/demo/fulldemoStatic/woman5.png,https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/meta/demo/fulldemoStatic/woman6.png,https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/meta/demo/fulldemoStatic/woman7.png,https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/meta/demo/fulldemoStatic/woman8.png,https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/meta/demo/fulldemoStatic/woman9.png";

    public static String getName() {
        List<String> snList = Arrays.asList(nikeName.split(","));
        Random random = new Random();
        return snList.get(random.nextInt(snList.size() - 1));
    }

    public static String headImage() {
        List<String> snList = Arrays.asList(headImage.split(","));
        Random random = new Random();
        return snList.get(random.nextInt(snList.size() - 1));
    }
}
