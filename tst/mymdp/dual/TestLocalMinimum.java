package mymdp.dual;

import java.util.Arrays;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import mymdp.core.Action;
import mymdp.core.MDPIP;
import mymdp.core.Policy;
import mymdp.core.SolutionReport;
import mymdp.core.UtilityFunction;
import mymdp.problem.ImprecisionGenerator;
import mymdp.problem.ImprecisionGeneratorImpl;
import mymdp.problem.MDPImpreciseFileProblemReader;
import mymdp.solver.ValueIterationIPImpl;

@RunWith(Parameterized.class)
public class TestLocalMinimum
{
	private static final double MAX_ERROR = 0.001;

	private static final Logger log = LogManager.getLogger(TestLocalMinimum.class);

	private static final String PROBLEMS_DIR = "precise_problems";
	private static final double MAX_RELAXATION = 0.15;

	@Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][]{
				{"local_minimum_navigation.net", MAX_RELAXATION}});
	}

	private final String filename;
	private final MDPIP mdpip;
	private final ImprecisionGeneratorImpl initialProblemImprecisionGenerator;

	public TestLocalMinimum(final String filename, final double maxRelaxation) {
		this.filename = filename;
		// Reads the MDP's definition from file and turns it to an imprecise
		// problem
		log.info("Current Problem: {}", filename);
		initialProblemImprecisionGenerator = new ImprecisionGeneratorImpl(maxRelaxation);
		mdpip = MDPImpreciseFileProblemReader.readFromFile("precise_problems\\" + filename, initialProblemImprecisionGenerator);
		// log.info("Initial problem is {}", mdpip);
		log.info("Problem read.");
	}

	@Test
	public void dual() {
		final SolutionReport report = new DualGame(MAX_ERROR).solve(new Problem<MDPIP,ImprecisionGenerator>() {
			@Override
			public String getName() {
				return filename;
			}

			@Override
			public MDPIP getModel() {
				return mdpip;
			}

			@Override
			public ImprecisionGenerator getComplement() {
				return initialProblemImprecisionGenerator;
			}
		});
		log.info(report);
		createVectorField(report.getPolicyResult(), report.getValueResult());
	}

	@Test
	public void value() {
		final UtilityFunction report = new ValueIterationIPImpl().solve(
				MDPImpreciseFileProblemReader.readFromFile(PROBLEMS_DIR + "\\" + filename, new ImprecisionGeneratorImpl(MAX_RELAXATION)),
				MAX_ERROR);
		log.info(report);
		// createVectorField(report.getPolicyResult());
	}

	@Test
	public void satiaPolicyIteration() {
		final SolutionReport report = new PolicyIterationSatiaGame(MAX_ERROR).solve(new Problem<MDPIP,Void>() {
			@Override
			public String getName() {
				return filename;
			}

			@Override
			public MDPIP getModel() {
				return mdpip;
			}

			@Override
			public Void getComplement() {
				return null;
			}
		});
		log.info(report);
		createVectorField(report.getPolicyResult(), report.getValueResult());
	}

	private static final String[] states = new String[]{"robot-at-x01y01",
			"robot-at-x01y02",
			"robot-at-x01y03",
			"robot-at-x02y01",
			"robot-at-x02y02",
			"robot-at-x02y03",
			"robot-at-x03y01",
			"robot-at-x03y02",
			"robot-at-x03y03",
			"robot-at-x04y01",
			"robot-at-x04y02",
			"robot-at-x04y03"};

	private static void createVectorField(final Policy policy, final UtilityFunction value) {
		for ( final String state : states ) {
			final int x = Integer.parseInt(state.substring(10, 12));
			final int y = Integer.parseInt(state.substring(13));
			final int[] delta = action(policy.getActionFor(state));

			log.info(x + 0.5 - delta[0] / 4.0 + " " + ( y + 0.5 - delta[1] / 4.0 ) + " " + delta[0] / 2.0 + " " + delta[1] / 2.0);
			log.info("set label \"" + value.getUtility(state) + "\" at " + ( x + 0.5 ) + "," + ( y + 0.5 ));
		}
	}

	private static int[] action(final Action action) {
		switch ( action.name() ) {
			case "move-north":
				return new int[]{0, 1};
			case "move-west":
				return new int[]{1, 0};
			case "move-east":
				return new int[]{-1, 0};
			case "move-south":
				return new int[]{0, -1};
			default:
				throw new IllegalStateException();
		}
	}
}
