package cn.lc.sunnyside.Service.ServiceImpl;

import cn.lc.sunnyside.POJO.DO.FamilyUser;
import cn.lc.sunnyside.POJO.DO.HealthRecord;
import cn.lc.sunnyside.mapper.FamilyElderRelationMapper;
import cn.lc.sunnyside.mapper.FamilyUserMapper;
import cn.lc.sunnyside.mapper.HealthRecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthRecordServiceImplTest {

    @Mock
    private FamilyUserMapper familyUserMapper;

    @Mock
    private FamilyElderRelationMapper familyElderRelationMapper;

    @Mock
    private HealthRecordMapper healthRecordMapper;

    @InjectMocks
    private HealthRecordServiceImpl healthRecordService;

    @Test
    void queryElderHealthShouldReturnNoPermissionWhenRelationMissing() {
        FamilyUser user = new FamilyUser();
        user.setId(1L);
        when(familyUserMapper.selectByPhone("13800000000")).thenReturn(user);
        when(familyElderRelationMapper.existsRelation(1L, 10L)).thenReturn(0);

        String result = healthRecordService.queryElderHealth("13800000000", 10L, LocalDate.now(), LocalDate.now());

        assertThat(result).contains("不存在绑定关系");
    }

    @Test
    void queryElderHealthShouldReturnSummaryWhenRecordsExist() {
        FamilyUser user = new FamilyUser();
        user.setId(1L);
        when(familyUserMapper.selectByPhone("13800000000")).thenReturn(user);
        when(familyElderRelationMapper.existsRelation(1L, 10L)).thenReturn(1);

        LocalDate date = LocalDate.of(2026, 3, 22);
        HealthRecord first = new HealthRecord();
        first.setRecordDate(date);
        first.setRecordTime(LocalTime.of(9, 0));
        first.setSystolicBp(130);
        first.setDiastolicBp(85);
        first.setHeartRate(72);
        first.setBloodSugar(new BigDecimal("6.10"));
        first.setTemperature(new BigDecimal("36.6"));

        HealthRecord second = new HealthRecord();
        second.setRecordDate(date);
        second.setRecordTime(LocalTime.of(10, 0));
        second.setSystolicBp(128);
        second.setDiastolicBp(82);
        second.setHeartRate(70);
        second.setBloodSugar(new BigDecimal("5.90"));
        second.setTemperature(new BigDecimal("36.7"));

        when(healthRecordMapper.selectByElderIdAndDateRange(10L, date, date)).thenReturn(List.of(second, first));

        String result = healthRecordService.queryElderHealth("13800000000", 10L, date, date);

        assertThat(result).contains("记录条数:2");
        assertThat(result).contains("均值");
    }
}
