package cn.lc.sunnyside.Service;

import cn.lc.sunnyside.POJO.DO.ElderlyUser;
import java.util.List;

public interface ElderlyUserService {
    String getElderLocation(Long elderId);
    String getHealthReminder(Long elderId);
    ElderlyUser getById(Long id);
    ElderlyUser findElderlyUserBySurname(String surname);
    List<ElderlyUser> findByRef(String ref);
}
