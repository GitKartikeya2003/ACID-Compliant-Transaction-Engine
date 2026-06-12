package com.banking.netBankingBackend.util;


import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AESAttributeConvertor implements AttributeConverter<String, String> {
    @Override
    public String convertToDatabaseColumn(String attribute) {
       if(attribute == null) return "";

       return AESUtil.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
    if(dbData == null)      return "";

    return AESUtil.decrypt(dbData);
    }
}
