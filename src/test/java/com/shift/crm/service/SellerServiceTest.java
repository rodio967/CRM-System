package com.shift.crm.service;

import com.shift.crm.dto.request.SellerCreateRequest;
import com.shift.crm.dto.request.SellerUpdateRequest;
import com.shift.crm.dto.response.SellerResponse;
import com.shift.crm.exception.ResourceNotFoundException;
import com.shift.crm.mapper.SellerMapper;
import com.shift.crm.model.Seller;
import com.shift.crm.repository.SellerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerServiceTest {

    @Mock
    private SellerRepository sellerRepository;

    private SellerMapper sellerMapper;
    private Clock fixedClock;
    private SellerService service;

    @BeforeEach
    void setUp() {
        sellerMapper = new SellerMapper();
        fixedClock = Clock.fixed(
                Instant.parse("2026-05-24T10:00:00Z"),
                ZoneId.of("UTC")
        );
        service = new SellerService(sellerRepository, sellerMapper, fixedClock);
    }

    @Test
    void findAll_returnsMappedSellers() {
        Seller seller = new Seller("Иван", "ivan@example.com", LocalDateTime.now(fixedClock));
        when(sellerRepository.findAllByDeletedFalse()).thenReturn(List.of(seller));

        List<SellerResponse> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Иван");
    }

    @Test
    void findById_returnsSeller() {
        Seller seller = new Seller("Иван", "ivan@example.com", LocalDateTime.now(fixedClock));
        when(sellerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(seller));

        SellerResponse result = service.findById(1L);

        assertThat(result.name()).isEqualTo("Иван");
    }

    @Test
    void findById_throwsWhenSellerNotFound() {
        when(sellerRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(100L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("100");
    }

    @Test
    void create_setsRegistrationDateFromClock() {
        when(sellerRepository.save(any(Seller.class))).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<Seller> captor = ArgumentCaptor.forClass(Seller.class);

        service.create(new SellerCreateRequest("Иван", "ivan@example.com"));

        verify(sellerRepository).save(captor.capture());
        assertThat(captor.getValue().getRegistrationDate())
                .isEqualTo(LocalDateTime.now(fixedClock));
    }

    @Test
    void create_copiesFieldsFromRequest() {
        when(sellerRepository.save(any(Seller.class))).thenAnswer(inv -> inv.getArgument(0));

        SellerResponse result = service.create(new SellerCreateRequest("Иван", "ivan@example.com"));

        assertThat(result.name()).isEqualTo("Иван");
        assertThat(result.contactInfo()).isEqualTo("ivan@example.com");
    }

    @Test
    void update_changesOnlyContactInfoWhenNameIsNull() {
        Seller seller = new Seller("Иван", "old@x", LocalDateTime.now(fixedClock));
        when(sellerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(seller));

        service.update(1L, new SellerUpdateRequest(null, "new@x"));

        assertThat(seller.getName()).isEqualTo("Иван");
        assertThat(seller.getContactInfo()).isEqualTo("new@x");
    }

    @Test
    void update_changesOnlyNameWhenContactInfoIsNull() {
        Seller seller = new Seller("Иван", "ivan@example.com", LocalDateTime.now(fixedClock));
        when(sellerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(seller));

        service.update(1L, new SellerUpdateRequest("Пётр", null));

        assertThat(seller.getName()).isEqualTo("Пётр");
        assertThat(seller.getContactInfo()).isEqualTo("ivan@example.com");
    }

    @Test
    void update_changesBothFieldsWhenProvided() {
        Seller seller = new Seller("Иван", "old@x", LocalDateTime.now(fixedClock));
        when(sellerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(seller));

        service.update(1L, new SellerUpdateRequest("Пётр", "new@x"));

        assertThat(seller.getName()).isEqualTo("Пётр");
        assertThat(seller.getContactInfo()).isEqualTo("new@x");
    }

    @Test
    void update_throwsWhenSellerNotFound() {
        when(sellerRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(100L, new SellerUpdateRequest("Пётр", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_marksSellerAsDeletedAndDoesNotRemoveFromDatabase() {
        Seller seller = new Seller("Иван", "ivan@example.com", LocalDateTime.now(fixedClock));
        when(sellerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(seller));

        service.delete(1L);

        assertThat(seller.isDeleted()).isTrue();
        verify(sellerRepository, never()).delete(any());
    }

    @Test
    void delete_throwsWhenSellerNotFound() {
        when(sellerRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(100L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getActiveSellerOrThrow_throwsForDeletedSeller() {
        when(sellerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getActiveSellerOrThrow(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
