package com.example.sensorcollect;

import java.util.UUID;

/**
 * Created by xie on 14-11-10.
 */
public class SensorBean {
    private Integer id;
    private Double accX;
    private Double accY;
    private Double accZ;
    private Double gyroX;
    private Double gyroY;
    private Double gyroZ;
    private Double magnetX;
    private Double magnetY;
    private Double magnetZ;
    private Double orientX;
    private Double orientY;
    private Double orientZ;
    private Double gravityX;
    private Double gravityY;
    private Double gravityZ;
    private Double linearAccX;
    private Double linearAccY;
    private Double linearAccZ;
    private String type;
    private String position;
    private String timestamp;
    private UUID uuid;
    private int seq;
    private String imei;
    private String direction;



    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Double getAccX() {
        return accX;
    }

    public void setAccX(Double accX) {
        this.accX = accX;
    }

    public Double getAccY() {
        return accY;
    }

    public void setAccY(Double accY) {
        this.accY = accY;
    }

    public Double getAccZ() {
        return accZ;
    }

    public void setAccZ(Double accZ) {
        this.accZ = accZ;
    }

    public Double getGyroX() {
        return gyroX;
    }

    public void setGyroX(Double gyroX) {
        this.gyroX = gyroX;
    }

    public Double getGyroY() {
        return gyroY;
    }

    public void setGyroY(Double gyroY) {
        this.gyroY = gyroY;
    }

    public Double getGyroZ() {
        return gyroZ;
    }

    public void setGyroZ(Double gyroZ) {
        this.gyroZ = gyroZ;
    }

    public Double getMagnetX() {
        return magnetX;
    }

    public void setMagnetX(Double magnetX) {
        this.magnetX = magnetX;
    }

    public Double getMagnetY() {
        return magnetY;
    }

    public void setMagnetY(Double magnetY) {
        this.magnetY = magnetY;
    }

    public Double getMagnetZ() {
        return magnetZ;
    }

    public void setMagnetZ(Double magnetZ) {
        this.magnetZ = magnetZ;
    }

    public Double getOrientX() {
        return orientX;
    }

    public void setOrientX(Double orientX) {
        this.orientX = orientX;
    }

    public Double getOrientY() {
        return orientY;
    }

    public void setOrientY(Double orientY) {
        this.orientY = orientY;
    }

    public Double getOrientZ() {
        return orientZ;
    }

    public void setOrientZ(Double orientZ) {
        this.orientZ = orientZ;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public Double getGravityX() {
        return gravityX;
    }

    public void setGravityX(Double gravityX) {
        this.gravityX = gravityX;
    }

    public Double getGravityY() {
        return gravityY;
    }

    public void setGravityY(Double gravityY) {
        this.gravityY = gravityY;
    }

    public Double getGravityZ() {
        return gravityZ;
    }

    public void setGravityZ(Double gravityZ) {
        this.gravityZ = gravityZ;
    }

    public Double getLinearAccX() {
        return linearAccX;
    }

    public void setLinearAccX(Double linearAccX) {
        this.linearAccX = linearAccX;
    }

    public Double getLinearAccY() {
        return linearAccY;
    }

    public void setLinearAccY(Double linearAccY) {
        this.linearAccY = linearAccY;
    }

    public Double getLinearAccZ() {
        return linearAccZ;
    }

    public void setLinearAccZ(Double linearAccZ) {
        this.linearAccZ = linearAccZ;
    }
}
