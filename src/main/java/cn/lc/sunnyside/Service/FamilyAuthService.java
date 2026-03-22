package cn.lc.sunnyside.Service;

import cn.lc.sunnyside.POJO.DTO.FamilyAuthDTO;

public interface FamilyAuthService {
    FamilyAuthDTO.FamilyLoginResponse login(FamilyAuthDTO.FamilyLoginRequest request);
}
