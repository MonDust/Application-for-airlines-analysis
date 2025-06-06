package pg.edu.pl.lsea.gui.display.topNdisplay;

import pg.edu.pl.lsea.entities.Output;

import java.util.*;

import static pg.edu.pl.lsea.utils.guispecific.optionmapping.AnalysisTypeConstants.NUMBER_OF_MOST_POPULAR_OPERATORS;

/**
 * Class responsible for returning percentage of long flights for Top N Operators - showing a windows with an appropriate message.
 */
public class TopNOperatorsPercentageDisplay extends BaseTopN {

    /**
     * Constructor for the class.
     */
    public TopNOperatorsPercentageDisplay() {
        displayTopOperatorsPercentage();
    }

    /**
     * Method to perform the percentage of long flights for Top N operators analysis and display the results.
     * n established by constant.
     */
    public void displayTopOperatorsPercentage() {
        // TODO - Make it receive results of corresponding analysis and display them accordingly

        List<Output> percentages = dataLoader.getTopNPercentageOfLongFlights(NUMBER_OF_MOST_POPULAR_OPERATORS);
                //new ArrayList();

        //Output o = new Output("PlaceholderIcao", 0);
        //percentages.add(o);

        log("Top " + percentages.size() + " Operators:");
        for (int i = 0; i < percentages.size(); i++) {
            // get it with Aircraft and Output
            String Operator = dataLoader.getAircraftIcao(percentages.get(i).getIcao24()).getOperator();
                    //"PlaceholderOperator";

            log("Operator " + Operator + ": " + percentages.get(i).getValue() + " %");
        }
    }
}
