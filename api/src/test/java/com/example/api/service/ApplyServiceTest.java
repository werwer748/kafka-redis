package com.example.api.service;

import com.example.api.repository.CouponRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ApplyServiceTest {

    @Autowired
    ApplyService applyService;

    @Autowired
    private CouponRepository couponRepository;

    @Test // 쿠폰 생성 확인
    public void 한번만응모() {
        applyService.apply(1L);

        long count = couponRepository.count();

        assertThat(count).isEqualTo(1);
    }

    @Test // 동시에 여러 요청이 올 때 테스트
    public void 여러명응모() throws InterruptedException {
        int threadCount = 1000;
        //? ExecutorService: 병렬작업을 간단하게 할 수 있도록 도와주는 java api
        ExecutorService executorService = Executors.newFixedThreadPool(32); // 멀티스레드 활용

        //? CountDownLatch: 다른 스레드에서 수행중인 작업을 기다리도록 도와주는 class
        CountDownLatch latch = new CountDownLatch(threadCount); // 다른 요청이 끝날때까지 기다려야 함.

        for (int i = 0; i < threadCount; i++) { // 1000개 요청
            long userId = i;
            executorService.submit(() -> {
                try {
                    applyService.apply(userId);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        Thread.sleep(10000);

        long count = couponRepository.count();

        /**
         * 테스트가 실패함
         * 레이스 컨디션이 발생하기 때문이다!
         *
         * 레이스 컨디션?
         * 두개 이상의 스레드가 공유데이터에 엑세스를 하고 동시에 작업을 하려고 할 때 발생하는 문제다.
         *
         * 레디스 사용 후 테스트가 성공
         * 레디스는 싱글스레드로 동작하기 때문에 다른 스레드에서 작업이 끝나느걸 기다리게 되기 떄문
         * 정상동작하고 문제가 없어 보일 수 있으나 발급하는 쿠폰의 양이 많을 경우 DB에 무리를 줄 수도 있음(이 DB를 사용중인 서비스들에 타격...)
         *
         * 카프카를 통해 컨슈머 프로젝트에서 insert를 수행하는경우
         * 작업이 끝나기 전 테스트가 끝나고있어서 테스트케이스가 실패함.. => Thread.sleep(10000) 으로 작업 종료시까지 대기 => 테스트 성공
         * api에서 직접 쿠폰을 생성하는것에 비해 처리량을 조절할 수 있어서 DB의 부담을 줄일 수 있다.
         * 다만, 테스트 케이스처럼 실제 로직의 완료까지 텀이 발생한다...
         */
        assertThat(count).isEqualTo(100);
    }

    @Test // 한명이 1000개 요청을 보냄. 쿠폰은 하나만 발급되어야 함.
    public void 한명당_한개의_쿠폰만_발급() throws InterruptedException {
        int threadCount = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) { // 1000개 요청
            long userId = i;
            executorService.submit(() -> {
                try {
                    applyService.apply(1L);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        Thread.sleep(10000);

        long count = couponRepository.count();

        assertThat(count).isEqualTo(1);
    }
}