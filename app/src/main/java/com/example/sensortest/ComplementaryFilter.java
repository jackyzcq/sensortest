package com.example.sensortest;


public class ComplementaryFilter {
    private double pitch = 0.0;  // 俯仰角 (Pitch)
    private double roll = 0.0;   // 横滚角 (Roll)
    private final double alpha;  // 互补滤波系数（如 0.98）

    public ComplementaryFilter(double alpha) {
        this.alpha = alpha;
    }

    public void update(double ax, double ay, double az, double gyroX, double gyroY, double dt) {
        // 计算加速度计的俯仰角 & 横滚角（基于重力加速度）
        double accPitch = Math.toDegrees(Math.atan2(ay, Math.sqrt(ax * ax + az * az)));
        double accRoll  = Math.toDegrees(Math.atan2(-ax, Math.sqrt(ay * ay + az * az)));

        // 陀螺仪角度积分计算
        double gyroPitch = pitch + gyroX * dt;
        double gyroRoll  = roll + gyroY * dt;

        // 互补滤波公式
        pitch = alpha * gyroPitch + (1 - alpha) * accPitch;
        roll  = alpha * gyroRoll  + (1 - alpha) * accRoll;
    }

    public double getPitch() {
        return pitch;
    }

    public double getRoll() {
        return roll;
    }

//    public static void main(String[] args) {
//        ComplementaryFilter filter = new ComplementaryFilter(0.98);
//
//        // 模拟传感器数据输入（单位：m/s² 和 deg/s）
//        double ax = 0.0, ay = 9.81, az = 0.0;  // 仅重力沿 Y 轴
//        double gyroX = 0.1, gyroY = -0.1;     // 陀螺仪角速度
//        double dt = 0.01;                     // 采样间隔 10ms
//
//        for (int i = 0; i < 100; i++) {
//            filter.update(ax, ay, az, gyroX, gyroY, dt);
//            System.out.printf("Pitch: %.2f°, Roll: %.2f°\n", filter.getPitch(), filter.getRoll());
//        }
//    }
}

