package com.github.hcsp.annotation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;


public class CacheClassDecorator {
    // 将传入的服务类Class进行增强
    // 使得返回一个具有如下功能的Class：
    // 如果某个方法标注了@Cache注解，则返回值能够被自动缓存注解所指定的时长
    // 这意味着，在短时间内调用同一个服务的同一个@Cache方法两次
    // 它实际上只被调用一次，第二次的结果直接从缓存中获取
    // 注意，缓存的实现需要是线程安全的
    public static <T> Class<? extends T> decorate(Class<T> klass) {
        return new ByteBuddy()
                .subclass(klass)
                .method(ElementMatchers.isAnnotatedWith(Cache.class))
                .intercept(MethodDelegation.to(CacheAdvisor.class))
                .make()
                .load(klass.getClassLoader())
                .getLoaded();
    }

    public static class CacheAdvisor {
        private static final ConcurrentHashMap<CacheKey, CacheValue> cache = new ConcurrentHashMap<>();

        @RuntimeType
        public static Object cache(
                @SuperCall Callable<Object> superCall,
                @Origin Method method,
                @This Object thisObj,
                @AllArguments Object[] arguments) throws Exception {
            CacheKey cacheKey = new CacheKey(thisObj, method.getName(), arguments);
            CacheValue resultExistingInCache = cache.get(cacheKey);
            //当前有缓存
            if (resultExistingInCache != null) {
                if (cacheExpires(resultExistingInCache, method)) {
                    //有缓存但是缓存过期，重新调用源方法
                    return invokeRealMethodAndPutIntoCache(superCall, cacheKey);
                } else {
                    //有缓存且没过期，返回缓存值
                    return resultExistingInCache.value;
                }
            } else {
                //没有缓存，调用源方法
                return invokeRealMethodAndPutIntoCache(superCall, cacheKey);
            }
        }

        private static Object invokeRealMethodAndPutIntoCache(@SuperCall Callable<Object> superCall,
                                                              CacheKey cacheKey) throws Exception {
            Object realMethodInvocationResult = superCall.call();
            cache.put(cacheKey, new CacheValue(realMethodInvocationResult, System.currentTimeMillis()));
            return realMethodInvocationResult;
        }

        private static boolean cacheExpires(CacheValue cacheValue, Method method) {
            long time = cacheValue.time;
            int cacheSeconds = method.getAnnotation(Cache.class).cacheSeconds();
            return System.currentTimeMillis() - time > cacheSeconds * 1000L;
        }
    }

    private static class CacheKey {
        private Object thisObj;
        private String methodName;
        private Object[] arguments;

        CacheKey(Object thisObj, String methodName, Object[] arguments) {
            this.thisObj = thisObj;
            this.methodName = methodName;
            this.arguments = arguments;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CacheKey cacheKey = (CacheKey) o;
            return Objects.equals(thisObj, cacheKey.thisObj) && Objects.equals(methodName, cacheKey.methodName) && Arrays.equals(arguments, cacheKey.arguments);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(thisObj, methodName);
            result = 31 * result + Arrays.hashCode(arguments);
            return result;
        }
    }

    private static class CacheValue {
        private final Object value;
        private final long time;

        CacheValue(Object value, long time) {
            this.value = value;
            this.time = time;
        }
    }

    public static void main(String[] args) throws Exception {
        DataService dataService = decorate(DataService.class).getConstructor().newInstance();

        // 有缓存的查询：只有第一次执行了真正的查询操作，第二次从缓存中获取
        System.out.println(dataService.queryData(1));
        Thread.sleep(1 * 1000);
        System.out.println(dataService.queryData(1));
        Thread.sleep(3 * 1000);
        System.out.println(dataService.queryData(1));

        // 无缓存的查询：两次都执行了真正的查询操作
        System.out.println(dataService.queryDataWithoutCache(1));
        Thread.sleep(1 * 1000);
        System.out.println(dataService.queryDataWithoutCache(1));
    }
}
