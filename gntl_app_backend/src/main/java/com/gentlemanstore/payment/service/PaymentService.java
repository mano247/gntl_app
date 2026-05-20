package com.gentlemanstore.payment.service;

import com.gentlemanstore.common.exception.ResourceNotFoundException;
import com.gentlemanstore.order.model.Order;
import com.gentlemanstore.order.repository.OrderRepository;
import com.gentlemanstore.payment.dto.CreatePaymentRequest;
import com.gentlemanstore.payment.dto.PaymentDTO;
import com.gentlemanstore.payment.mapper.PaymentMapper;
import com.gentlemanstore.payment.model.Payment;
import com.gentlemanstore.payment.model.PaymentMethod;
import com.gentlemanstore.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository repo;
    private final OrderRepository orderRepository;
    private final PaymentMapper mapper;

    @Transactional(readOnly = true)
    public PaymentDTO getPayment(Long orderId){
        Payment payment = repo.findByOrderIdAndDeletedFalse(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        return mapper.toDTO(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentDTO> getAllPayments(Pageable pageable) {
        return repo.findAllByDeletedFalse(pageable)
                .map(mapper::toDTO);
    }

    @Transactional()
    public PaymentDTO createPayment(CreatePaymentRequest request){
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        Payment payment = Payment.builder()
                .amount(order.getTotalPrice())
                .paymentMethod(PaymentMethod.valueOf(request.getPaymentMethod()))
                .status("PENDING")
                .order(order)
                .deleted(false)
                .build();

        repo.save(payment);
        return mapper.toDTO(payment);
    }

    @Transactional()
    public PaymentDTO updatePaymentStatus(Long id, String status){
        Payment payment = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        payment.setStatus(status);
        repo.save(payment);
        return mapper.toDTO(payment);
    }

    @Transactional()
    public void deletePayment(Long id){
        Payment payment = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        payment.setDeleted(true);
        repo.save(payment);
    }
}
