package co.com.crediya_solicitud.consumer.mapper;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class RestConsumerDtoMapperTest {

    private final RestConsumerDtoMapper mapper = Mappers.getMapper(RestConsumerDtoMapper.class);

//    @Test
//    void mapsAllFields_whenPojo() {
//        BigDecimal salario= new BigDecimal(100000);
//        ExternalUserDto dto = new ExternalUserDto("1111","Ana","perez","11-10-2025","111111","ana@mail.com","11111",salario);
//
//        User u = mapper.toDomain(dto);
//
//        Assertions.assertThat(u.email()).isEqualTo("ana@mail.com");
//        Assertions.assertThat(u.firstName()).isEqualTo("Ana");
//
//    }

    @Test
    void returnsNull_whenSourceNull() {
        Assertions.assertThat(mapper.toDomain(null)).isNull();
    }
}