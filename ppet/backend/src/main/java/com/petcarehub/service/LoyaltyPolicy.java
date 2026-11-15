package com.petcarehub.service;
import com.petcarehub.entity.*;
import com.petcarehub.enums.MembershipLevel;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class LoyaltyPolicy  {
    private final EntityManager em;
    private final SettingsService settings;
    public LoyaltyRules current() {
        var r=em.find(LoyaltyRules.class,1);
        return r==null?new LoyaltyRules():r;
    }
    public Map<String,Integer> thresholds() {
        var r=current();
        return settings.thresholds();
    }
    public void classify(LoyaltyAccount a) {
        var r=current();
        int total=a.getTotalPoints();
        a.setMembershipLevel(total>=r.getPlatinum()?MembershipLevel.PLATINUM:total>=r.getGold()?MembershipLevel.GOLD:total>=r.getSilver()?MembershipLevel.SILVER:MembershipLevel.BRONZE);
    }
}
