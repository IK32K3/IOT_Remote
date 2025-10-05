package com.example.iot.domain.usecase


import com.example.iot.data.MqttRepository
import javax.inject.Inject


class PublishUseCase @Inject constructor(
    private val repo: MqttRepository
) {
    operator fun invoke(topic: String, payload: String){
        repo.publish(topic, payload)
    }
}