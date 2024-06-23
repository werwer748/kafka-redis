package com.example.api.service;

import com.example.api.domain.Coupon;
import com.example.api.producer.CouponCreateProducer;
import com.example.api.repository.AppliedUserRepository;
import com.example.api.repository.CouponCountRepository;
import com.example.api.repository.CouponRepository;
import org.springframework.stereotype.Service;

@Service
public class ApplyService {

    private final CouponRepository couponRepository;
    private final CouponCountRepository couponCountRepository;
    private final CouponCreateProducer couponCreateProducer;
    private final AppliedUserRepository appliedUserRepository;

    public ApplyService(
            CouponRepository couponRepository,
            CouponCountRepository couponCountRepository,
            CouponCreateProducer couponCreateProducer, AppliedUserRepository appliedUserRepository
    ) {
        this.couponRepository = couponRepository;
        this.couponCountRepository = couponCountRepository;
        this.couponCreateProducer = couponCreateProducer;
        this.appliedUserRepository = appliedUserRepository;
    }

    public void apply(Long userId) {
//        long count = couponRepository.count(); // 기존 레디스 사용전 쌩짜로 카운트 가져오기
        // redis에 incr이라는 명령어 => key에 대한 value를 1씩 증가 시킨다.
//        Long count = couponCountRepository.increment();
        // 쿠폰은 1인당 1개가 발급되어야한다. 이 문제를 해결하기위해 redis에서 set 자료구조를 통해 중복데이터를 제거하자.
        // set으로 만든 자료에 데이터를 넣고 리턴된 값으로 처리된 데이터를 확인할 수 있음 여기서 1이 아니면 중복되거나 정상적으로 레디스에 등록된 데이터가 아님.
        Long apply = appliedUserRepository.add(userId);

        if (apply != 1) {
            return;
        }

        Long count = couponCountRepository.increment();

        if (count > 100) {
            return;
        }

        // 카프카 설치 후 직접 쿠폰을 생성하는 로직은 없앰
//        couponRepository.save(new Coupon(userId));
        couponCreateProducer.create(userId);
    }
}
