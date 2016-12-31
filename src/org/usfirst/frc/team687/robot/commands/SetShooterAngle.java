package org.usfirst.frc.team687.robot.commands;

import org.usfirst.frc.team687.robot.Robot;
import org.usfirst.frc.team687.robot.constants.DrivetrainConstants;
import org.usfirst.frc.team687.robot.constants.IntakeConstants;
import org.usfirst.frc.team687.robot.constants.ShooterConstants;
import org.usfirst.frc.team687.robot.constants.ShooterConstants.Location;
import org.usfirst.frc.team687.robot.utilities.MotionProfileGenerator;

import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.command.Command;

/**
 * Adjust shooter angle to specified encoder position (ticks) 
 * Using motion profiled feedforward and PD feedback
 * 
 * @author tedfoodlin
 *
 */

public class SetShooterAngle extends Command {

	private double m_startPos;
	private double m_setpoint;
	private double m_setpointPos;
	private double m_angle;
	
	private double m_startingTime;
	
	private MotionProfileGenerator m_motionProfileGenerator;
	private double m_vmax = IntakeConstants.kAngleMaxVelocity;
	private double m_amax = IntakeConstants.kAngleMaxAccel;
	private double m_dmax = IntakeConstants.kAngleMaxDecel;
	
	private double m_error;
	private double m_lastError;
	
    public SetShooterAngle(Location location){
    	m_setpoint = ShooterConstants.getShooterAngle(location);
    	
        // subsystem dependencies
        requires(Robot.shooter);
    }
	
	@Override
	protected void initialize() {
    	m_startPos = Robot.shooter.getCurrentAngle();
    	m_angle = m_setpoint - m_startPos;
    	
    	m_motionProfileGenerator = null;
    	m_motionProfileGenerator = new MotionProfileGenerator(m_vmax, m_amax, m_dmax);
    	m_motionProfileGenerator.generateProfile(m_angle);
    	
    	Robot.shooter.resetSensors();
    	m_startingTime = Timer.getFPGATimestamp();
    	m_error = 0;
	}

	@Override
	protected void execute() {
    	m_lastError = m_error;
    	double currentTime = Timer.getFPGATimestamp() - m_startingTime;
		double goalVelocity = m_motionProfileGenerator.readVelocity(currentTime);
		double goalAcceleration = m_motionProfileGenerator.readAcceleration(currentTime);
		m_setpointPos = m_motionProfileGenerator.readDistance(currentTime);
		double actualPos = Robot.shooter.getCurrentAngle();
		m_error = m_setpointPos - actualPos;
		double pow = DrivetrainConstants.kDriveTranslationP * m_error 
				+ DrivetrainConstants.kDriveTranslationD * ((m_error - m_lastError) / currentTime - goalVelocity)
				+ DrivetrainConstants.kV * goalVelocity 
				+ DrivetrainConstants.kA * goalAcceleration;
		
		Robot.shooter.setLifterPower(pow);;
	}

	@Override
	protected boolean isFinished() {
		return Math.abs(m_setpoint - Robot.shooter.getCurrentAngle()) < 5;
	}

	@Override
	protected void end() {
    	m_motionProfileGenerator = null;
	}

	@Override
	protected void interrupted() {
		m_setpointPos = Robot.shooter.getCurrentAngle();
		m_motionProfileGenerator = null;
	}

}
