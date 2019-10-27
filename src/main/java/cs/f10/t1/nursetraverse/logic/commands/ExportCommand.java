package cs.f10.t1.nursetraverse.logic.commands;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import cs.f10.t1.nursetraverse.commons.core.Messages;
import cs.f10.t1.nursetraverse.commons.core.index.Index;
import cs.f10.t1.nursetraverse.importexport.CsvUtil;
import cs.f10.t1.nursetraverse.importexport.exceptions.ExportingException;
import cs.f10.t1.nursetraverse.logic.commands.exceptions.CommandException;
import cs.f10.t1.nursetraverse.logic.parser.CliSyntax;
import cs.f10.t1.nursetraverse.model.Model;
import cs.f10.t1.nursetraverse.model.patient.Patient;

/**
 * Exports Patient data from the PatientBook
 */

public class ExportCommand extends Command {

    public static final String COMMAND_WORD = "app-export";

    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Exports patients data into a .csv file in /exports.\n"
            + "Patients are exported selectively by index.\n"
            + "If indexes are not provided, all patients will be exported.\n"
            + "File name must be new - overriding an existing file is not permitted.\n"
            + "Parameters: [" + CliSyntax.PREFIX_FILENAME + "FILENAME] "
            + "[" + CliSyntax.PREFIX_INDEXES + "INDEXES]...\n"
            + "Example: " + COMMAND_WORD + " " + CliSyntax.PREFIX_FILENAME + "patient_data "
            + CliSyntax.PREFIX_INDEXES + "2 " + CliSyntax.PREFIX_INDEXES + "4 " + CliSyntax.PREFIX_INDEXES + "6";

    public static final String MESSAGE_SUCCESS = "Export success! File written to: ";
    public static final String MESSAGE_FAILURE = "Export failed.";
    public static final String MESSAGE_EMPTY = "Nothing to export!";
    public static final String MESSAGE_FILE_EXISTS =
            "File name already in use. Please delete the existing file or use a new file name";

    private final String exportFileName;
    private final Optional<Set<Index>> targetIndexes;

    /**
     * @param exportFileName of the .csv to create and export to
     * @param targetIndexes of the patients to selectively export, if any
     */
    public ExportCommand(String exportFileName, Optional<Set<Index>> targetIndexes) {
        this.exportFileName = exportFileName;
        this.targetIndexes = targetIndexes;
    }

    /**
     * export all patients specified by {@Code targetIndexes}
     * if no indexes are provided, all patients data are exported.
     */
    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);

        List<Patient> lastShownList = model.getStagedPatientList();

        if (lastShownList.isEmpty()) {
            throw new CommandException(MESSAGE_EMPTY);
        }

        // Selectively export if indexes are provided
        if (targetIndexes.isPresent()) {

            if (!indexesAllInBounds(targetIndexes.get(), lastShownList.size())) {
                // If any index is out of bounds, throw an exception
                throw new CommandException(Messages.MESSAGE_INVALID_PATIENT_DISPLAYED_INDEX);
            }
            // Else get a list of patients at the specified indexes
            lastShownList = model.getPatientsByIndexes(targetIndexes.get());
        }

        String pathString;
        try {
            pathString = CsvUtil.writePatientsToCsv(lastShownList, exportFileName);
        } catch (ExportingException e) {
            throw new CommandException(MESSAGE_FILE_EXISTS);
        } catch (IOException e) {
            throw new CommandException(MESSAGE_FAILURE);
        }

        return new CommandResult(MESSAGE_SUCCESS + pathString);
    }

    /**
     * Returns true if indexes in the set are all within bounds.
     */
    private boolean indexesAllInBounds(Set<Index> indexes, int bound) {
        for (Index index : indexes) {
            if (index.getZeroBased() >= bound) {
                return false;
            }
        }
        return true;
    }
}