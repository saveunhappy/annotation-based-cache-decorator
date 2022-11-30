package com.github.hcsp.annotation;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import net.bytebuddy.ByteBuddy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CacheClassDecoratorTest {
    private TestDataService testDataService;

    @BeforeEach
    public void setUp() throws Exception {
        testDataService =
                CacheClassDecorator.decorate(TestDataService.class).getConstructor().newInstance();
    }

    @Test
    public void sameParameterCanBeCached() throws Exception {
        List<Object> first = testDataService.cachedMethod(1, "A");
        Thread.sleep(1000);
        List<Object> second = testDataService.cachedMethod(1, "A");

        Assertions.assertEquals(first, second);
    }

    @Test
    public void sameParameterNotCachedAfterExpiration() throws Exception {
        List<Object> first = testDataService.cachedMethod(1, "A");
        Thread.sleep(6000);
        List<Object> second = testDataService.cachedMethod(1, "A");

        Assertions.assertNotEquals(first, second);
    }

    @Test
    public void differentParameterNotCached() throws Exception {
        List<Object> first = testDataService.cachedMethod(1, "A");
        List<Object> second = testDataService.cachedMethod(2, "A");

        Assertions.assertNotEquals(first, second);
    }

    @Test
    public void nonCachedMethodWorks() throws Exception {
        List<Object> first = testDataService.nonCachedMethod(1, "A");
        List<Object> second = testDataService.nonCachedMethod(1, "A");

        Assertions.assertNotEquals(first, second);
    }

    @Test
    public void testqqq() throws Exception {
        LinkedList<String> queue = new LinkedList<>();
//        queue.addLast("111");
//        queue.addLast("222");
//        queue.addLast("333");
        //add 和 addLast没有大的区别，小的区别就是add会返回一个true
        queue.addLast("5555");
        queue.add("111");
        queue.add("222");
        queue.add("333");
        queue.add("444");
        for (String s : queue) {
            System.out.println(s);
        }
    }

    public static class TestDataService {
        @Cache(cacheSeconds = 5)
        public List<Object> cachedMethod(int param1, String param2) {
            return Arrays.asList(param1, param2, new Random().nextInt());
        }

        public List<Object> nonCachedMethod(int param1, String param2) {
            return Arrays.asList(param1, param2, new Random().nextInt());
        }
    }


}
