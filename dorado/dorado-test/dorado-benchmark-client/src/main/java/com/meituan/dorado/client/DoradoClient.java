package com.meituan.dorado.client;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.UniformReservoir;
import com.meituan.dorado.api.Echo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class DoradoClient {

    private final static Logger logger = LoggerFactory.getLogger(DoradoClient.class);

    public static final String base = "abcdefghijklmnopqrstuvwxyz0123456789";

    public static int threadNum = 64;
    public static long msgCount = 10000000;
    public static int msgLength = 1000;

    private AtomicLong totalCount = new AtomicLong(0);
    private AtomicLong errorCount = new AtomicLong(0);

    @Resource(name = "clientProxy")
    private Echo.Iface client;

    private Histogram histogram;

    @PostConstruct
    public void runClient() {

        histogram = new Histogram(new UniformReservoir(10000));
        totalCount = new AtomicLong(0);

        long start = System.currentTimeMillis();
        Thread[] threads = new Thread[threadNum];
        for (int i = 0; i < threadNum; i++) {
            threads[i] = new InvokeThread();
            threads[i].start();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        for (int i = 0; i < threadNum; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        long end = System.currentTimeMillis();

        logger.info("------Dorado:ThreadNum:" + threadNum + " MsgCount:" + msgCount + " MsgLength:" + msgLength + "------");
        logger.info("TPS:" + msgCount * 1000 / (end - start));
        logger.info("99th:" + histogram.getSnapshot().get99thPercentile());
        logger.info("95th:" + histogram.getSnapshot().get95thPercentile());
        logger.info("Mean:" + histogram.getSnapshot().getMean());
        logger.info("Median:" + histogram.getSnapshot().getMedian());
        logger.info("Max:" + histogram.getSnapshot().getMax());
        logger.info("Error Rate:" + errorCount.get() * 1.f / histogram.getCount() * 100 + "%");
        logger.info("check histogram count:" + histogram.getCount());
    }

    class InvokeThread extends Thread {

        private long id = 0;

        @Override
        public void run() {
            String message = getRandomString(msgLength);
            while (true) {
                id = totalCount.incrementAndGet();
                if (id > msgCount) {
                    break;
                }
                long before = System.currentTimeMillis();
                try {
                    client.echo(message);
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                }
                long after = System.currentTimeMillis();
                long cost = after - before;
                histogram.update(cost);
            }
        }
    }


    public static String getRandomString(int length) {
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        ClassPathXmlApplicationContext clientBeanFactory = new ClassPathXmlApplicationContext("benchmark/client.xml");
        Echo.Iface client = (Echo.Iface) clientBeanFactory.getBean("clientProxy");
        new DoradoClient().runClient();
    }
}
