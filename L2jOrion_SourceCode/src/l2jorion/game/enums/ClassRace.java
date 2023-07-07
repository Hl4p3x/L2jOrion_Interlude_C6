package l2jorion.game.enums;

public enum ClassRace
{
	HUMAN(1),
	ELF(1.5),
	DARK_ELF(1.5),
	ORC(0.9),
	DWARF(0.8);
	
	private final double _breathMultiplier;
	
	private ClassRace(double breathMultiplier)
	{
		_breathMultiplier = breathMultiplier;
	}
	
	public double getBreathMultiplier()
	{
		return _breathMultiplier;
	}
}