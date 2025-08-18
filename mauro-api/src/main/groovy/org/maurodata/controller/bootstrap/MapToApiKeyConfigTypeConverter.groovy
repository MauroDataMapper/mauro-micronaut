package org.maurodata.controller.bootstrap

import io.micronaut.core.convert.ConversionContext
import io.micronaut.core.convert.TypeConverter
import jakarta.inject.Singleton

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.LocalDateTime
import java.time.LocalDate

@Singleton
class MapToApiKeyConfigTypeConverter implements TypeConverter<Map, MauroConfiguration.ApiKeyConfig> {

    @Override
    Optional<MauroConfiguration.ApiKeyConfig> convert(Map object, Class<MauroConfiguration.ApiKeyConfig> targetType, ConversionContext context) {

        try {
            return Optional.of(new MauroConfiguration.ApiKeyConfig() {
                @Override
                String getName() {
                    return object.get("name")
                }

                @Override
                String getEmail() {
                    return object.get("email")
                }

                @Override
                UUID getKey() {
                    if (!object.containsKey("key")) {return null}
                    return UUID.fromString(object.get("key").toString())
                }

                @Override
                Boolean getRefreshable() {
                    Object refreshableObject = object.get("refreshable")
                    if (refreshableObject == null) {return false}
                    return Boolean.parseBoolean(object.get("refreshable").toString())
                }

                @Override
                Instant getExpiry() {
                    Object expiryObject = object.get("expiry")
                    if (expiryObject == null) {
                        return Instant.now().atOffset(ZoneOffset.UTC).plus(1, ChronoUnit.YEARS).toInstant()
                    }

                    String expiryString = expiryObject.toString().trim()

                    ZoneId zone = ZoneId.systemDefault()
                    try {
                        return LocalDateTime.parse(expiryString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            .atZone(zone).toInstant()
                    } catch (Exception e) {
                        return LocalDate.parse(expiryString, DateTimeFormatter.ISO_LOCAL_DATE)
                            .atStartOfDay(zone)
                            .plus(23, ChronoUnit.HOURS)
                            .plus(59, ChronoUnit.MINUTES)
                            .plus(59, ChronoUnit.SECONDS)
                            .toInstant()
                    }
                }
            })
        }
        catch (Exception e) {
            context.reject(object, e)
        }
        return Optional.empty()
    }
}
