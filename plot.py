import matplotlib.pyplot as plt
import numpy as np
import xarray as xr
import re
from pathlib import Path
import seaborn as sns
import pandas as pd
import matplotlib
import glob
import re
from datetime import datetime

def distance(val, ref):
    return abs(ref - val)


vectDistance = np.vectorize(distance)


def cmap_xmap(function, cmap):
    """ Applies function, on the indices of colormap cmap. Beware, function
    should map the [0, 1] segment to itself, or you are in for surprises.

    See also cmap_xmap.
    ""    """
    import matplotlib.pyplot as plt
    cmap = plt.get_cmap('viridis')
    cdict = cmap._segmentdata
    function_to_map = lambda x: (function(x[0]), x[1], x[2])
    for key in ('red', 'green', 'blue'):
        cdict[key] = map(function_to_map, cdict[key])
    #        cdict[key].sort()
    #        assert (cdict[key][0]<0 or cdict[key][-1]>1), "Resulting indices extend out of the [0, 1] segment."
    return matplotlib.colors.LinearSegmentedColormap('colormap', cdict, 1024)


def getClosest(sortedMatrix, column, val):
    while len(sortedMatrix) > 3:
        half = int(len(sortedMatrix) / 2)
        sortedMatrix = sortedMatrix[-half - 1:] if sortedMatrix[half, column] < val else sortedMatrix[: half + 1]
    if len(sortedMatrix) == 1:
        result = sortedMatrix[0].copy()
        result[column] = val
        return result
    else:
        safecopy = sortedMatrix.copy()
        safecopy[:, column] = vectDistance(safecopy[:, column], val)
        minidx = np.argmin(safecopy[:, column])
        safecopy = safecopy[minidx, :].A1
        safecopy[column] = val
        return safecopy


def convert(column, samples, matrix):
    return np.matrix([getClosest(matrix, column, t) for t in samples])


def valueOrEmptySet(k, d):
    return (d[k] if isinstance(d[k], set) else {d[k]}) if k in d else set()


def mergeDicts(d1, d2):
    """
    Creates a new dictionary whose keys are the union of the keys of two
    dictionaries, and whose values are the union of values.

    Parameters
    ----------
    d1: dict
        dictionary whose values are sets
    d2: dict
        dictionary whose values are sets

    Returns
    -------
    dict
        A dict whose keys are the union of the keys of two dictionaries,
    and whose values are the union of values

    """
    res = {}
    for k in d1.keys() | d2.keys():
        res[k] = valueOrEmptySet(k, d1) | valueOrEmptySet(k, d2)
    return res


def extractCoordinates(filename):
    """
    Scans the header of an Alchemist file in search of the variables.

    Parameters
    ----------
    filename : str
        path to the target file
    mergewith : dict
        a dictionary whose dimensions will be merged with the returned one

    Returns
    -------
    dict
        A dictionary whose keys are strings (coordinate name) and values are
        lists (set of variable values)

    """
    with open(filename, 'r') as file:
        #        regex = re.compile(' (?P<varName>[a-zA-Z._-]+) = (?P<varValue>[-+]?\d*\.?\d+(?:[eE][-+]?\d+)?),?')
        regex = r"(?P<varName>[a-zA-Z._-]+) = (?P<varValue>(?:\[[^\]]*\]|[^,]*)),?"
        dataBegin = r"\d"
        is_float = r"[-+]?\d*\.?\d+(?:[eE][-+]?\d+)?"
        for line in file:
            match = re.findall(regex, line.replace('Infinity', '1e30000'))
            if match:
                return {
                    var: float(value) if re.match(is_float, value)
                    else bool(re.match(r".*?true.*?", value.lower())) if re.match(r".*?(true|false).*?", value.lower())
                    else value
                    for var, value in match
                }
            elif re.match(dataBegin, line[0]):
                return {}


def extractVariableNames(filename):
    """
    Gets the variable names from the Alchemist data files header.

    Parameters
    ----------
    filename : str
        path to the target file

    Returns
    -------
    list of list
        A matrix with the values of the csv file

    """
    with open(filename, 'r') as file:
        dataBegin = re.compile('\d')
        lastHeaderLine = ''
        for line in file:
            if dataBegin.match(line[0]):
                break
            else:
                lastHeaderLine = line
        if lastHeaderLine:
            regex = re.compile(" (?P<varName>\S+)")
            return regex.findall(lastHeaderLine)
        return []


def openCsv(path):
    """
    Converts an Alchemist export file into a list of lists representing the matrix of values.

    Parameters
    ----------
    path : str
        path to the target file

    Returns
    -------
    list of list
        A matrix with the values of the csv file

    """
    regex = re.compile('\d')
    with open(path, 'r') as file:
        lines = filter(lambda x: regex.match(x[0]), file.readlines())
        return [[float(x) for x in line.split()] for line in lines]


def beautifyValue(v):
    """
    Converts an object to a better version for printing, in particular:
        - if the object converts to float, then its float value is used
        - if the object can be rounded to int, then the int value is preferred

    Parameters
    ----------
    v : object
        the object to try to beautify

    Returns
    -------
    object or float or int
        the beautified value
    """
    try:
        v = float(v)
        if v.is_integer():
            return int(v)
        return v
    except:
        return v

import os

if __name__ == '__main__':
    current_experiment = "gossip-sm"

    directory = 'data'
    # Where to save charts
    current_datetime = pd.Timestamp.now().strftime('%Y-%m-%d_%H-%M-%S')
    output_directory = f'charts' #_{current_datetime}
    os.makedirs(output_directory, exist_ok=True)
    # How to name the summary of the processed data
    pickleOutput = f'gossip-sm'
    # Experiment prefixes: one per experiment (root of the file name)
    experiments = ['self-stab-gossip-sm', 'non-stab-gossip-sm', 'time-rep-gossip-sm'] #'messages-self-construction-fixed-leader',
    floatPrecision = '{: 0.3f}'
    # Number of time samples
    timeSamples = 200
    # time management
    minTime = 0
    maxTime = 200
    timeColumnName = 'time'
    logarithmicTime = False
    # One or more variables are considered random and "flattened"
    seedVars = ['seed'] #, 'maxResource', 'maxSuccess', 'resourceLowerBound'
    # Label mapping

    # Setup libraries
    np.set_printoptions(formatter={'float': floatPrecision.format})
    # Read the last time the data was processed, reprocess only if new data exists, otherwise just load
    import pickle
    import os

    if os.path.exists(directory):
        newestFileTime = max([os.path.getmtime(directory + '/' + file) for file in os.listdir(directory)], default=0.0)
        try:
            lastTimeProcessed = pickle.load(open('timeprocessed', 'rb'))
        except:
            lastTimeProcessed = -1
        shouldRecompute = not os.path.exists(".skip_data_process") and newestFileTime != lastTimeProcessed
        if not shouldRecompute:
            try:
                means = pickle.load(open(pickleOutput + '_mean', 'rb'))
                stdevs = pickle.load(open(pickleOutput + '_std', 'rb'))
            except:
                shouldRecompute = True
        if shouldRecompute:
            timefun = np.logspace if logarithmicTime else np.linspace
            means = {}
            stdevs = {}
            for experiment in experiments:
                # Collect all files for the experiment of interest
                import fnmatch
                allfiles = filter(lambda file: fnmatch.fnmatch(file, experiment + '_*.csv'), os.listdir(f'{directory}/{experiment}'))
                allfiles = [directory + f'/{experiment}/' + name for name in allfiles]
                allfiles.sort()
                # From the file name, extract the independent variables
                dimensions = {}
                for file in allfiles:
                    dimensions = mergeDicts(dimensions, extractCoordinates(file))
                dimensions = {k: sorted(v) for k, v in dimensions.items()}
                # Add time to the independent variables
                dimensions[timeColumnName] = range(0, timeSamples)
                # Compute the matrix shape
                shape = tuple(len(v) for k, v in dimensions.items())
                # Prepare the Dataset
                dataset = xr.Dataset()
                for k, v in dimensions.items():
                    dataset.coords[k] = v
                if len(allfiles) == 0:
                    print("WARNING: No data for experiment " + experiment)
                    means[experiment] = dataset
                    stdevs[experiment] = xr.Dataset()
                else:
                    varNames = extractVariableNames(allfiles[0])
                    for v in varNames:
                        if v != timeColumnName:
                            novals = np.ndarray(shape)
                            novals.fill(float('nan'))
                            dataset[v] = (dimensions.keys(), novals)
                    # Compute maximum and minimum time, create the resample
                    timeColumn = varNames.index(timeColumnName)
                    allData = {file: np.matrix(openCsv(file)) for file in allfiles}
                    computeMin = minTime is None
                    computeMax = maxTime is None
                    if computeMax:
                        maxTime = float('-inf')
                        for data in allData.values():
                            maxTime = max(maxTime, data[-1, timeColumn])
                    if computeMin:
                        minTime = float('inf')
                        for data in allData.values():
                            minTime = min(minTime, data[0, timeColumn])
                    timeline = timefun(minTime, maxTime, timeSamples)
                    # Resample
                    for file in allData:
                        #                    print(file)
                        allData[file] = convert(timeColumn, timeline, allData[file])
                    # Populate the dataset
                    for file, data in allData.items():
                        dataset[timeColumnName] = timeline
                        for idx, v in enumerate(varNames):
                            if v != timeColumnName:
                                darray = dataset[v]
                                experimentVars = extractCoordinates(file)
                                darray.loc[experimentVars] = data[:, idx].A1


                    # Fold the dataset along the seed variables, producing the mean and stdev datasets
                    mergingVariables = [seed for seed in seedVars if seed in dataset.coords]
                    means[experiment] = dataset.mean(dim=mergingVariables, skipna=True)
                    stdevs[experiment] = dataset.std(dim=mergingVariables, skipna=True)
            # Save the datasets
            pickle.dump(means, open(pickleOutput + '_mean', 'wb'), protocol=-1)
            pickle.dump(stdevs, open(pickleOutput + '_std', 'wb'), protocol=-1)
            pickle.dump(newestFileTime, open('timeprocessed', 'wb'))
    else:
        means = {experiment: xr.Dataset() for experiment in experiments}
        stdevs = {experiment: xr.Dataset() for experiment in experiments}

    for experiment in experiments:
        current_experiment_means = means[experiment]
        current_experiment_errors = stdevs[experiment]



# Custom charting
# plot in a single boxplot chart by using seaborn, the data of both experiments "classic-vmc" and "field-vmc-fixed-leader",
# comparing the stabilization time of the two experiments. The x-axis should represent the experiment name,
# and the y-axis should represent the stabilization time. The title of the chart should be "Stabilization Time Comparison".
# the stabilization time is the amount of time elapsed from the start of the experiment to the end of the experiment.

def plot_selfs(data, experiment, metric, y_label='', walk=''):
    i = len(data)+2
    plt.rcParams.update({'font.size': 15})
    plt.rcParams.update({'legend.loc': 0})
    colors = sns.color_palette("viridis", n_colors=i)
    fig, ax1 = plt.subplots(figsize=(9, 5))
    what = ''
    for j, ((_, initNodes, findM), (mean_df, std_df)) in enumerate(data.items()):
        sns.lineplot(
            data=mean_df,
            x='time',
            y=metric,
            label=f'$N_0 = {initNodes}$',
            color=colors[j+2],
        )
        upper_bound = mean_df[metric] + std_df[f'{metric}-std']
        lower_bound = mean_df[metric] - std_df[f'{metric}-std']
        plt.fill_between(mean_df['time'], lower_bound, upper_bound, color=colors[j+2], alpha=0.2)
        if findM:
            what = "MaxOf"
        else:
            what = "MinOf"

    ax1.set_xlim(0, 200)
    cut_event = 50
    change_range = 100
    merge_event = 150
    plt.axvline(x=cut_event, color=colors[2], linestyle='--', linewidth=1, label='Cut Event')
    plt.axvline(x=change_range, color=colors[1], linestyle='dotted', linewidth=1, label='Change Range')
    plt.axvline(x=merge_event, color=colors[0], linestyle='--', linewidth=1, label='Merge Event')
    walk = ''
    if 'walk' in experiment:
        walk = 'Brownian Walk '
    plt.title(f'{beautify_experiment_name(experiment, walk)} ({what})')
    plt.xlabel('Simulated seconds')
    plt.ylabel(y_label + beautify_metric_name(metric))
    plt.legend(prop={'size': 8})
    plt.tight_layout()
    filename = beautify_experiment(experiment, metric)
    plt.savefig(f'{output_directory}/{walk}{filename}-{what}.pdf', dpi=300)
    # plt.show()
    plt.close()

def beautify_metric_name(name):
    if name == 'RMSE':
        return 'Root Mean Squared Error'
    elif name == 'MAE':
        return 'Mean Absolute Error'
    elif name == 'MEAN':
        return 'Mean'
    elif name == 'MessageSize[mean]':
        return 'Average Message Size'
    elif name == 'MessageSize[Sum]':
        return 'Overall Data Rate'
    elif name == 'nodes':
        return 'Number of Nodes'
    else:
        raise Exception(f'Unknown metric name {name}.')

def beautify_experiment_name(name, walk=''):
    if 'non-stab-gossip' in name:
        return f'Non Stabilizing Gossip {walk}'
    elif 'self-stab-gossip' in name:
        return f'Self Stabilizing Gossip {walk}'
    elif 'time-rep-gossip' in name:
        return f'Time Replicated Gossip {walk}'
    else:
        raise Exception(f'Unknown experiment name {name}.')

def beautify_experiment(experiment, metric, walk=''):
    return beautify_experiment_name(experiment, walk) + f' ({beautify_metric_name(metric)})'

def plot_data_rate(data, experiment, metric):
    plt.rcParams.update({'font.size': 15})
    plt.rcParams.update({'legend.loc': 0})
    colors = sns.color_palette("viridis", n_colors=len(data) + 2)

    fig, ax1 = plt.subplots(figsize=(9, 5))
    what = ''

    for j, (((_, findM), (_, initNodes)), (mean_df, std_df)) in enumerate(data.items()):
        sns.lineplot(
            data=mean_df,
            x='time',
            y=metric,
            label=f'{initNodes * 2}',
            ax=ax1,
            color=colors[j+2],
        )
        upper_bound = mean_df[metric] + std_df[f'{metric}-std']
        lower_bound = mean_df[metric] - std_df[f'{metric}-std']
        plt.fill_between(mean_df['time'], lower_bound, upper_bound, color=colors[j+2], alpha=0.2)
        if findM:
            what = "MaxOf"
        else:
            what = "MinOf"
    ax1.set_xlim(0, 200)
    ax1.set_ylim(0, None)
    ylabel = beautify_metric_name(metric)
    if 'Sum' in metric:
        ylabel += ' (KB/s)'
        # ax1.set_yscale('log')
        # ticks = np.append(np.append(np.linspace(1, 9, num=9), np.linspace(10, 90, num=9)), np.linspace(100, 900, num=9))
        # ax1.set_ylim(1,800)
        # ax1.set_yticks(ticks)
        #ax1.set_yticklabels([str(int(t)) if i % 2 == 0 else '' for i, t in enumerate(ticks)])
    if 'mean' in metric:
        ylabel += ' (B)'
        # ylim = (600, 13000)
        # ax1.set_yscale('log')
        # ticks = np.append(np.linspace(600, 900, num=4), np.linspace(1000, 10000, num=10))
        # ax1.set_yticks(ticks)
        # ax1.set_yticklabels([str(int(t)) if i % 2 == 0 else '' for i, t in enumerate(ticks)])
        # ax1.set_ylim(ylim)
    ax1.set_xlabel('Simulated seconds')

    ax1.set_ylabel(f'{ylabel}')

    cut_event = 50
    change_range = 100
    merge_event = 150
    plt.axvline(x=cut_event, color=colors[2], linestyle='--', linewidth=1, label='Cut Event')
    plt.axvline(x=change_range, color=colors[1], linestyle='dotted', linewidth=1, label='Change Range')
    plt.axvline(x=merge_event, color=colors[0], linestyle='--', linewidth=1, label='Merge Event')

    handles, labels = ax1.get_legend_handles_labels()
    if handles:
        ax1.legend(handles, labels, title=r'$nodes$', loc=4)
        # ax1.legend(handles, labels, title='max resources (solid=KB, dashed=nodes)', loc='best')
    walk = ''
    if 'walk' in experiment:
        walk = 'Brownian Walk '
    plt.title(f'{beautify_experiment_name(experiment, walk)} ({what})')
    plt.tight_layout()
    filename = beautify_experiment(experiment, metric)
    plt.savefig(f'{output_directory}/{walk}{filename}-{what}.pdf', dpi=300)
    plt.close()

def plot_experiments_comparison(data, metric, nodes, selector, y_label='', walk=''):
    """
    Plot different experiments on the same chart for a fixed number of nodes and metric.
    """
    plt.rcParams.update({'font.size': 15})
    plt.rcParams.update({'legend.loc': 0})

    colors = sns.color_palette("viridis", n_colors=len(data) + 2)
    fig, ax1 = plt.subplots(figsize=(9, 5))

    for j, (experiment, (mean_df, std_df)) in enumerate(data.items()):
        sns.lineplot(
            data=mean_df,
            x='time',
            y=metric,
            label=beautify_experiment_name(experiment),
            ax=ax1,
            color=colors[j + 2],
        )

        upper = mean_df[metric] + std_df[f'{metric}-std']
        lower = mean_df[metric] - std_df[f'{metric}-std']
        ax1.fill_between(mean_df['time'], lower, upper, color=colors[j + 2], alpha=0.2)

    ax1.set_xlim(0, 200)
    ax1.set_ylim(-0.1, None)
    ax1.set_xlabel('Simulated seconds')
    ax1.set_ylabel(beautify_metric_name(metric) + y_label)

    # Event markers (same semantics as other plots)
    plt.axvline(x=50, color=colors[2], linestyle='--', linewidth=1, label='Cut Event')
    plt.axvline(x=100, color=colors[1], linestyle='dotted', linewidth=1, label='Change Range')
    plt.axvline(x=150, color=colors[0], linestyle='--', linewidth=1, label='Merge Event')

    what = "MaxOf" if selector else "MinOf"
    ax1.set_title(f'{beautify_metric_name(metric)} – N₀={nodes * 2} ({what})')

    ax1.legend(prop={'size': 9})
    plt.tight_layout()
    plt.savefig(
        f'{output_directory}/comparison-{walk}{nodes * 2}nodes-{metric}-{what}.pdf',
        dpi=300
    )
    plt.close()

from matplotlib import pyplot as plt
simType = ['sm']
metrics = ['RMSE'] #'MEAN',, 'MAE'
totalNodes = [2, 10, 50, 100]
findMax = [True, False]
experiments = ['self-stab-gossip-sm', 'non-stab-gossip-sm', 'time-rep-gossip-sm']

for experiment in experiments:
    for metric_to_plot in metrics:
        for selector in findMax:
            data_dict = {}
            for nodes in totalNodes:
                mean = np.where(np.isnan(means[experiment][metric_to_plot].sel(dict(findMax=selector, totalNodes=nodes)).values), 0.0, means[experiment][metric_to_plot].sel(dict(findMax=selector, totalNodes=nodes)).values)
                nodes_series = np.where(np.isnan(means[experiment]['nodes'].sel(dict(findMax=selector, totalNodes=nodes)).values), 0.0, means[experiment]['nodes'].sel(dict(findMax=selector, totalNodes=nodes)).values)
                time_series = means[experiment].sel(dict(findMax=selector, totalNodes=nodes))['time'].values

                df_mean = pd.DataFrame({
                    'time': time_series,
                    f'{metric_to_plot}': mean,
                    'nodes': nodes_series,
                })

                df_std = pd.DataFrame({
                    'time': time_series,
                    f'{metric_to_plot}-std': stdevs[experiment][metric_to_plot].sel(dict(findMax=selector, totalNodes=nodes)).values,
                    'nodes-std': stdevs[experiment]['nodes'].sel(dict(findMax=selector, totalNodes=nodes)).values,
                })

                data_dict[(f"{nodes}", nodes * 2, selector)] = (df_mean, df_std)
                exp_name = beautify_experiment_name(experiment)

            walk=''
            if 'walk' in experiment:
                walk = 'Brownian Walk '
            plot_selfs(data_dict, experiment=experiment, metric=metric_to_plot, walk=walk)

data_dict = {}
metric_to_plot = ['MessageSize[mean]', 'MessageSize[Sum]']

for metric in metric_to_plot:
    for experiment in experiments:
        for selector in findMax:
            data_dict = {}
            for initNodes in totalNodes:
                mean_mean = np.where(np.isnan(means[experiment]['MessageSize[mean]'].sel(dict(findMax=selector, totalNodes=initNodes)).values), 0.0, means[experiment]['MessageSize[mean]'].sel(dict(findMax=selector, totalNodes=initNodes)).values) # / 1024
                mean_sum = np.where(np.isnan(means[experiment]['MessageSize[Sum]'].sel(dict(findMax=selector, totalNodes=initNodes)).values), 0.0, means[experiment]['MessageSize[Sum]'].sel(dict(findMax=selector, totalNodes=initNodes)).values) / 1024
                nodes_series = np.where(np.isnan(means[experiment]['nodes'].sel(dict(findMax=selector, totalNodes=initNodes)).values), 0.0, means[experiment]['nodes'].sel(dict(findMax=selector, totalNodes=initNodes)).values)
                time_series = means[experiment]['MessageSize[mean]'].sel(dict(findMax=selector, totalNodes=initNodes))['time'].values

                df_mean = pd.DataFrame({
                    'time': time_series,
                    'MessageSize[mean]': mean_mean,
                    'MessageSize[Sum]': mean_sum,
                    'nodes': nodes_series,
                })

                df_std = pd.DataFrame({
                    'time': time_series,
                    'MessageSize[mean]-std': stdevs[experiment]['MessageSize[mean]'].sel(dict(findMax=selector, totalNodes=initNodes)).values, # / 1024,
                    'MessageSize[Sum]-std': stdevs[experiment]['MessageSize[Sum]'].sel(dict(findMax=selector, totalNodes=initNodes)).values / 1024,
                    'nodes-std': stdevs[experiment]['nodes'].sel(dict(findMax=selector, totalNodes=initNodes)).values,
                })

                data_dict[(f"findMax-{selector}", selector),(f"nodes-{initNodes}", initNodes)] = (df_mean, df_std)
            plot_data_rate(data_dict, experiment=experiment, metric=metric)
# find max value of message size mean and sum across all experiments for setting y axis limit
max_message_size_mean = 0
max_message_size_sum = 0
for experiment in experiments:
    if 'MessageSize[mean]' in means[experiment]:
        max_message_size_mean = max(max_message_size_mean, np.nanmax(means[experiment]['MessageSize[mean]'].values))
    if 'MessageSize[Sum]' in means[experiment]:
        max_message_size_sum = max(max_message_size_sum, np.nanmax(means[experiment]['MessageSize[Sum]'].values) / 1024)
print(f'Maximum average message size across all experiments: {max_message_size_mean} B')
print(f'Maximum total message size across all experiments: {max_message_size_sum} KB/s')
#find also the minimum non-zero value
min_nonzero_mean = float('inf')
min_nonzero_sum = float('inf')
for experiment in experiments:
    if 'MessageSize[mean]' in means[experiment]:
        nonzero_values = means[experiment]['MessageSize[mean]'].values[means[experiment]['MessageSize[mean]'].values > 0]
        if len(nonzero_values) > 0:
            min_nonzero_mean = min(min_nonzero_mean, np.nanmin(nonzero_values))
    if 'MessageSize[Sum]' in means[experiment]:
        nonzero_values = means[experiment]['MessageSize[Sum]'].values[means[experiment]['MessageSize[Sum]'].values > 0] / 1024
        if len(nonzero_values) > 0:
            min_nonzero_sum = min(min_nonzero_sum, np.nanmin(nonzero_values))
if min_nonzero_mean != float('inf'):
    print(f'Minimum non-zero average message size across all experiments: {min_nonzero_mean} B')
if min_nonzero_sum != float('inf'):
    print(f'Minimum non-zero total message size across all experiments: {min_nonzero_sum} KB/s')


#convert it in logscale
if max_message_size_mean > 0:
    print(f'Log scale: {np.log10(max_message_size_mean)}')
if max_message_size_sum > 0:
    print(f'Log scale: {np.log10(max_message_size_sum)}')

for sim in simType:
    for metric_to_plot in metrics:
        for selector in findMax:
            for nodes in totalNodes:

                comparison_data = {}
                walk=''
                experimentsType = [exp for exp in experiments if sim in exp]
                for experiment in experimentsType:
                    mean = np.where(
                        np.isnan(
                            means[experiment][metric_to_plot]
                            .sel(dict(findMax=selector, totalNodes=nodes))
                            .values
                        ),
                        0.0,
                        means[experiment][metric_to_plot]
                        .sel(dict(findMax=selector, totalNodes=nodes))
                        .values
                    )

                    time_series = (
                        means[experiment][metric_to_plot]
                        .sel(dict(findMax=selector, totalNodes=nodes))
                        ['time']
                        .values
                    )

                    df_mean = pd.DataFrame({
                        'time': time_series,
                        metric_to_plot: mean,
                    })

                    df_std = pd.DataFrame({
                        'time': time_series,
                        f'{metric_to_plot}-std': (
                            stdevs[experiment][metric_to_plot]
                            .sel(dict(findMax=selector, totalNodes=nodes))
                            .values
                        ),
                    })

                    comparison_data[experiment] = (df_mean, df_std)

                    if 'walk' in experiment:
                        walk = 'Brownian Walk-'

                plot_experiments_comparison(
                    comparison_data,
                    metric=metric_to_plot,
                    nodes=nodes,
                    selector=selector,
                    walk = walk,
                )

# Compare experiments for fixed number of nodes – Message size metrics
message_metrics = ['MessageSize[mean]', 'MessageSize[Sum]']
for sim in simType:
    for metric_to_plot in message_metrics:
        for selector in findMax:
            for nodes in totalNodes:

                comparison_data = {}

                experimentsType = [exp for exp in experiments if sim in exp]
                for experiment in experimentsType:
                    raw_mean = means[experiment][metric_to_plot].sel(
                        dict(findMax=selector, totalNodes=nodes)
                    ).values

                    # Unit handling (consistent with existing plots)
                    if metric_to_plot == 'MessageSize[Sum]':
                        raw_mean = raw_mean / 1024.0  # KB/s

                    mean = np.where(np.isnan(raw_mean), 0.0, raw_mean)

                    time_series = (
                        means[experiment][metric_to_plot]
                        .sel(dict(findMax=selector, totalNodes=nodes))
                        ['time']
                        .values
                    )

                    df_mean = pd.DataFrame({
                        'time': time_series,
                        metric_to_plot: mean,
                    })

                    raw_std = stdevs[experiment][metric_to_plot].sel(
                        dict(findMax=selector, totalNodes=nodes)
                    ).values

                    if metric_to_plot == 'MessageSize[Sum]':
                        raw_std = raw_std / 1024.0  # KB/s

                    df_std = pd.DataFrame({
                        'time': time_series,
                        f'{metric_to_plot}-std': raw_std,
                    })

                    comparison_data[experiment] = (df_mean, df_std)

                ylabel = ' (KB/s)' if metric_to_plot == 'MessageSize[Sum]' else ' (B)'

                walk=''
                if 'walk' in experiment:
                    walk = 'Brownian Walk-'

                plot_experiments_comparison(
                    comparison_data,
                    metric=metric_to_plot,
                    nodes=nodes,
                    selector=selector,
                    y_label=ylabel,
                    walk = walk,
                )