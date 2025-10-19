package com.reservation.service.reservation_system.service

import com.reservation.service.reservation_system.repository.CityMappingRepository
import com.reservation.service.reservation_system.repository.entity.CityMapping
import org.springframework.stereotype.Service

@Service
class CityMappingService(
    private val cityMappingRepository: CityMappingRepository
) {

    fun createCityMapping(cityCode: String, cityName: String): CityMapping? {
        // Check if city code or name already exists
        if (cityMappingRepository.existsByCityCode(cityCode)) {
            throw IllegalArgumentException("City code '$cityCode' already exists")
        }
        if (cityMappingRepository.existsByCityName(cityName)) {
            throw IllegalArgumentException("City name '$cityName' already exists")
        }
        
        val cityMapping = CityMapping(
            cityCode = cityCode.uppercase(),
            cityName = cityName
        )
        return cityMappingRepository.save(cityMapping)
    }

    fun getCityByCode(cityCode: String): CityMapping? {
        return cityMappingRepository.findByCityCode(cityCode.uppercase())
    }

    fun getCityByName(cityName: String): CityMapping? {
        return cityMappingRepository.findByCityName(cityName)
    }

    fun searchCitiesByName(cityName: String): List<CityMapping> {
        return cityMappingRepository.findByCityNameContainingIgnoreCase(cityName)
    }

    fun getAllCities(): List<CityMapping> {
        return cityMappingRepository.findAll()
    }

    fun updateCityMapping(id: Long, cityCode: String?, cityName: String?): CityMapping? {
        val existingCity = cityMappingRepository.findById(id).orElse(null) ?: return null
        
        // Check for conflicts if updating
        cityCode?.let { code ->
            if (code.uppercase() != existingCity.cityCode && 
                cityMappingRepository.existsByCityCode(code.uppercase())) {
                throw IllegalArgumentException("City code '$code' already exists")
            }
        }
        
        cityName?.let { name ->
            if (name != existingCity.cityName && 
                cityMappingRepository.existsByCityName(name)) {
                throw IllegalArgumentException("City name '$name' already exists")
            }
        }
        
        val updatedCity = existingCity.copy(
            cityCode = cityCode?.uppercase() ?: existingCity.cityCode,
            cityName = cityName ?: existingCity.cityName
        )
        
        return cityMappingRepository.save(updatedCity)
    }

    fun deleteCityMapping(id: Long): Boolean {
        return if (cityMappingRepository.existsById(id)) {
            cityMappingRepository.deleteById(id)
            true
        } else {
            false
        }
    }

    fun initializeDefaultCities() {
        val defaultCities = listOf(
            "DEL" to "New Delhi",
            "BOM" to "Mumbai", 
            "BLR" to "Bangalore",
            "MAA" to "Chennai",
            "CCU" to "Kolkata",
            "HYD" to "Hyderabad",
            "AMD" to "Ahmedabad",
            "PNQ" to "Pune",
            "GOI" to "Goa",
            "COK" to "Kochi",
            "JAI" to "Jaipur",
            "LKO" to "Lucknow",
            "PAT" to "Patna",
            "IXC" to "Chandigarh",
            "GAU" to "Guwahati"
        )
        
        defaultCities.forEach { (code, name) ->
            if (!cityMappingRepository.existsByCityCode(code)) {
                createCityMapping(code, name)
            }
        }
    }
}
