package com.petcarehub.service;
import com.fasterxml.jackson.databind.JsonNode;
import com.petcarehub.exception.BadRequestException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
/** Explicit bounded input access; no entity binding or reflection-based mutation. */ public record CatalogInput(JsonNode json)  {
    public CatalogInput  {
        if(json==null||!json.isObject())throw bad("Nội dung phải là đối tượng JSON");
    }
    public void keys(String allowed) {
        var keys=Set.of(allowed.split(","));
        json.fieldNames().forEachRemaining(k-> {
            if(!keys.contains(k))throw bad("Trường không được phép: "+k);
        }
        );
    }
    public String text(String key,int max,boolean required) {
        var v=json.get(key);
        if(v==null||v.isNull()||v.isTextual()&&v.asText().isBlank()) {
            if(required)throw bad(key+": bắt buộc");
            return null;
        }
        if(!v.isTextual()||v.asText().length()>max)throw bad(key+": độ dài tối đa "+max);
        return v.asText().strip();
    }
    public Long id(String key,boolean required) {
        var v=json.get(key);
        if(v==null||v.isNull()) {
            if(required)throw bad(key+": bắt buộc");
            return null;
        }
        if(!v.isIntegralNumber()||!v.canConvertToLong()||v.longValue()<1)throw bad(key+": ID không hợp lệ");
        return v.longValue();
    }
    public int integer(String key,int min,int max,int fallback) {
        var v=json.get(key);
        if(v==null||v.isNull())return fallback;
        if(!v.isIntegralNumber()||!v.canConvertToInt()||v.intValue()<min||v.intValue()>max)throw bad(key+": ngoài giới hạn "+min+"–"+max);
        return v.intValue();
    }
    public boolean bool(String key,boolean fallback) {
        var v=json.get(key);
        if(v==null||v.isNull())return fallback;
        if(!v.isBoolean())throw bad(key+": phải là boolean");
        return v.booleanValue();
    }
    public BigDecimal money(String key,boolean positive) {
        var v=json.get(key);
        if(v==null||!v.isNumber())throw bad(key+": phải là số");
        var n=v.decimalValue();
        if(n.scale()>2||n.precision()-n.scale()>12||n.signum()<(positive?1:0))throw bad(key+": tiền không hợp lệ");
        return n;
    }
    public <T extends Enum<T>>T choice(String key,Class<T> type) {
        try {
            return Enum.valueOf(type,text(key,50,true));
        }
        catch(IllegalArgumentException e) {
            throw bad(key+": giá trị không hợp lệ");
        }
    }
    public LocalDate date(String key,boolean required) {
        String s=text(key,10,required);
        try {
            return s==null?null:LocalDate.parse(s);
        }
        catch(Exception e) {
            throw bad(key+": ngày không hợp lệ");
        }
    }
    public LocalDateTime dateTime(String key) {
        try {
            return LocalDateTime.parse(text(key,30,true));
        }
        catch(Exception e) {
            throw bad(key+": thời gian không hợp lệ");
        }
    }
    public static BadRequestException bad(String message) {
        return new BadRequestException(message);
    }
    public static String fingerprint(String value) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        }
        catch(java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
