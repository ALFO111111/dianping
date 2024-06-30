package com.alfo;

import org.junit.jupiter.api.Test;

import java.util.Random;

public class eat {

    @Test
    public void eatPlace() {
        String[] dests = {"金贵二楼", "西北餐厅", "紫藤二楼", "米线"};

        Random random = new Random();
        System.out.println(dests[random.nextInt(dests.length)]);
    }
}
