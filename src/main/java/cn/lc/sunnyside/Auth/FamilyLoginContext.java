package cn.lc.sunnyside.Auth;

public record FamilyLoginContext(Long familyId, String phone, String username) {
    public boolean isValid() {
        return familyId != null && phone != null && !phone.isBlank();
    }
}
