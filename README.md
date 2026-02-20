# A Self-Stabilizing Min-Max Consensus via Path-loop Detection

This artifact is associated with the regular paper article submitted in the Main Track of the COORDINATION 2025 conference.


### Authors

| **Angela Cortecchia** (*)  | **Danilo Pianini** (*)  | **Mirko Viroli** (*)  |
|:--------------------------:|:-----------------------:|:-----------------------:|
| angela.cortecchia@unibo.it | danilo.pianini@unibo.it | mirko.viroli@unibo.it |

(*)
*Department of Computer Science and Engineering \
Alma Mater Studiorum --- Università di Bologna - Cesena, Italy*

### Table of Contents
- [About](#about)
  - [Experiments](#experiments)
- [Getting Started](#getting-started)
    - [Requirements](#requirements)
    - [Limitations](#limitations)
    - [Understanding the experiments](#understanding-the-experiments)
    - [Walk-through the experiments](#walk-through-the-experiments)
    - [Reproduce the entire experiment](#reproduce-the-entire-experiment)
        * [Simulation Graphical Interface](#simulation-graphical-interface)
        * [Extremely quick-start of a basic experiment -- `(ba|z|fi)?sh` users only](#extremely-quick-start-of-a-basic-experiment----bazfish-users-only)
        * [Reproduce the experiments through Gradle](#reproduce-the-experiments-through-gradle)
        * [Changing experiment's parameters](#changing-experiments-parameters)
    - [Project Structure](#project-structure)
    - [Simulation Entrypoints](#simulation-entrypoints)
    - [Reproduce the experiment results](#reproduce-the-experiment-results)
        * [Reproduce the experiments with containers (recommended)](#reproduce-the-experiments-with-containers-recommended)
        * [Reproduce natively](#reproduce-natively)
        * [Generate the charts](#generate-the-charts)

## About

In large-scale distributed systems, such as the Internet of Things (IoT),
min-max consensus algorithms provide a mechanism for collective coordination
by enabling nodes to converge on a "best" value produced by one of the participants in the computation.
However,
min-max consensus algorithms are monotonic and non-self-stabilizing by nature:
once a value is merged into the aggregate it cannot be retracted,
leading to propagation of stale or incorrect data in the presence of transient faults or topology changes.
In this work,
we propose a novel self-stabilizing min-max consensus algorithm
ensuring convergence to the best available value in the network
by propagating information along shortest valid paths.
Each gossip message carries a value and path of nodes that have acknowledged it,
enabling loop-freedom and natural pruning of obsolete contributions.
We rely on field-based coordination and specifically the Aggregate Computing
paradigm to present the algorithm, prove self-stabilization,
and
provide an implementation as a reusable library for the Collektive DSL.
%and finally empirically demonstrate that the proposed solution preserves the locality
%and lightweight nature of gossip while ensuring global consistency and robustness to disconnections.
This work contributes a foundational building block for resilient coordination in pervasive computing systems,
paving the way to more complex,
self-stabilizing distributed applications.

### Experiments

The evaluation is carried out through simulation in a spatial network where devices are placed in a two-dimensional area 
and communicate only with neighbors within a fixed communication range.
This produces a proximity-based topology: 
nodes form a multi-hop network whose structure depends entirely on their geometric distribution. 
There is no central coordination—each device periodically executes the gossip update using only locally available information.

At the start of each simulation, 
nodes hold independent values and begin exchanging information asynchronously. 
Through repeated local interactions, 
information propagates across the network and the system converges to the value selected by the gossip operator.
Once the network reaches a stable agreement, 
we deliberately introduce disruptions to test the self-stabilizing behavior under realistic dynamic conditions.

Figure below illustrates the self-stabilizing behavior of the algorithm under topology changes through a sequence of simulation snapshots. 
Each node is represented as a circle, 
and the color encodes the value currently held by that node (i.e., the output of the min-consensus computation). 
Nodes sharing the same color have already agreed on the same value.

|   ![starting_structure](./snapshots/gossip06.png)    |        ![self-organised_structure](./snapshots/gossip09.png)        |
|:----------------------------------------------------:|:-------------------------------------------------------------------:|
|     *Structure partitioned after stabilization*      |                *Re-stabilization after partitioning*                |
| ![structure_after_cutting](./snapshots/gossip10.png) | ![self-organised_structure_after_cutting](./snapshots/gossip11.png) |
|                 *Reconnection phase*                 |                     *Restored global agreement*                     | 

## Getting started

### Requirements

In order to successfully download and execute the graphical experiments are needed:
- Internet connection;
- [Git](https://git-scm.com);
- Linux, macOS and Windows systems capable of running [Java](https://www.oracle.com/java/technologies/javase/jdk19-archive-downloads.html) 17 (or higher);
- 4GB free space on disk (the system will automatically download its dependencies through Gradle);
- GPU with minimal OpenGL capabilities (OpenGL 2.0);
- 4GB RAM.

The project uses [Gradle](https://gradle.org) as a build tool,
and all the project dependencies are listed in the `gradle\libs.versions.toml` file.

### Limitations

- The experiments run in "batch mode" generate a lot of data,
  and the simulation may take a long time to finish (up to several hours) even with high-performance computers.
  We suggest running the experiments in "graphic mode" to have a better understanding of the simulation;
- On different monitor types with different resolutions, the graphical interface could appear a bit different;
- "batch mode" does not show any graphical interface;
- For GUI interpretation, please refer to the [Simulation Graphical Interface](#simulation-graphical-interface) section;
- Due to Alchemist's limitations, the graphical interface will not appear if run on a docker container.

### Understanding the experiments

The experiments are designed to evaluate the behavior of the proposed self-stabilizing min-max consensus algorithm under different conditions.
The main experiment, named _gossip_, simulates a basic gossip protocol where nodes exchange information and converge to a consensus value.
The other experiments, named _splitAndMergeNonStabGossip_, _splitAndMergeSelfStabGossip_, and _splitAndMergeTimeRepGossip_, 
simulate a scenario where the network is partitioned, the nodes values are changed, and then the network is reconnected,
to evaluate the self-stabilizing behavior of the algorithm under topology changes.

In all the experiments, nodes are represented as circles, and the color encodes the value currently held by that node (i.e., the output of the min-max-consensus computation).
Shared colors indicate agreement on the same value among nodes, while different colors represent different values.
Lines are communication links, indicating which nodes are within communication range and can exchange information.

The experiments are:
- _gossip_: basic gossip experiment;
- _splitAndMergeNonStabGossip_: non-stabilizing min/max gossip in a split & merge topology;
- _splitAndMergeSelfStabGossip_: the proposed self-stabilizing min/max gossip in a split & merge topology;
- _splitAndMergeTimeRepGossip_: non-stabilizing gossip wrapped by time replication for self-stabilization in a split & merge topology.

### Walk-through the experiments

This section provides a brief overview of the _gossip_ base experiment.
It is executed with the following default parameters:
- $50$ nodes in the simulation;
- communication range of $10m$ (i.e., nodes can communicate with robots within a radius of $10m$);
- the type of gossip selector is default to $min$.

The detailed instructions to reproduce the experiment are in the section "[Reproduce the entire experiment](#reproduce-the-entire-experiment)".

The simulation can be launched with the command `./gradlew runGossipGraphic` on a Unix terminal,
or the one in the section "[Extremely quick-start](#extremely-quick-start-of-a-basic-experiment----bazfish-users-only)".
Note that the optional `MAX_SEED` is a parameter used for batch experiments,
which allows running the experiment with a fixed number of seeds, but it does not affect the graphical experiments.

Once the simulation has started, the Alchemist GUI will open.
After Alchemist finishes loading, you will see the initial setup:
the nodes distributed throughout the environment, with `NaN` as their initial value.
The nodes may be initially hidden, depending on your monitor resolution and the default zoom level of the GUI, 
but they are present in the simulation, and it is just necessary to zoom out or move around the environment to see them.
To move around the environment, you can use the mouse to drag the view, and the scroll wheel to zoom in and out.
To move the nodes, press <kbd>S</kbd> to enter the "selection mode," then select the nodes you want to move by dragging a box around them, 
and then press <kbd>O</kbd> and move the selected nodes to the desired position.
All the shortcuts for the GUI can be found in the [Alchemist documentation](https://alchemistsimulator.github.io/reference/swing/index.html#shortcuts).

For more details of the simulation (e.g., the appearance, the meaning of the different colors, etc.)
see the section [Understanding the experiments](#understanding-the-experiments).
Now the simulation can be started by pressing the <kbd>P</kbd> key on the keyboard.
By pressing the <kbd>P</kbd> key again, the simulation will pause (and resume).
When the simulation starts,
if you wish to execute it at "real time" speed,
press the <kbd>R</kbd> key (and again to return to the fast speed).
Note that you can move the nodes at any time during the simulation, even when it is running, to see how the system reacts to topology changes.
For other features of the GUI, please refer to the [Simulation Graphical Interface](#simulation-graphical-interface) section.

### Reproduce the entire experiment

**WARNING**: re-running the whole experiment may take a very long time on a normal computer.

#### Simulation Graphical Interface

The simulation environment and graphical interface are provided by [Alchemist Simulator](https://alchemistsimulator.github.io/index.html).
To understand how to interact with the GUI,
please refer to the [Alchemist documentation](https://alchemistsimulator.github.io/reference/swing/index.html#shortcuts).

#### Extremely quick-start of a basic experiment -- `(ba|z|fi)?sh` users only

- Requires a Unix terminal (`(ba|z|fi)?sh`)
- `curl` must be installed
- run:
```bash
# simple gossip experiment with default parameters and graphical interface
curl https://raw.githubusercontent.com/angelacorte/experiments-coordination-self-stabilizing-gossip/master/run-gossip.sh | bash 
```
- the repository is in your `Downloads` folder for further inspection.

#### Reproduce the experiments through Gradle

1. Install a Gradle-compatible version of Java.
   Use the [Gradle/Java compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html) to learn which is the compatible version range.
   The Version of Gradle used in this experiment can be found in the gradle-wrapper.properties file located in the gradle/wrapper folder.
2. Open a terminal
3. Clone this repository on your pc with `git clone https://github.com/angelacorte/experiments-coordination-self-stabilizing-gossip`.
4. Move into the root folder with `cd experiments-coordination-self-stabilizing-gossip`
5. Depending on the platform, run the following command:
    - Bash compatible (Linux, Mac OS X, Git Bash, Cygwin): ``` ./gradlew run<ExperimentName>Graphic ```
    - Windows native (cmd.exe, Powershell): ``` gradlew.bat run<ExperimentName>Graphic ```
6. Substitute `<ExperimentName>` with the name of the experiment (in PascalCase) specified in the YAML simulation file.
   Or execute ```./gradlew tasks``` to view the list of available tasks.

**NOTES:**
- Due to Alchemist's limitations, the graphical interface will not appear if run on a docker container.
- The tasks *in graphic mode* will run the experiments with the default parameters.
- Graphic tasks run with the default parameters defined in the YAML.

**Note** that before each experiment command, it can be optionally set the `MAX_SEED` environment variable to a specific value to run the experiment,
since that parameter is relevant only for batch experiments,
it is suggested to not specify it or set it to `0` for the graphical experiments.
To change the selector function, you can prepend the command with `FIND_MAX=false` to use the `min` selector instead of the default `max`.

Depending on the platform, there may be different ways to set the environment variable:
- If you're using Bash compatible (Linux, Mac OS X, Git Bash, Cygwin): ```MAX_SEED=0 ./gradlew run<ExperimentName>Graphic```
- If you're using Command Prompt (cmd.exe): ```set MAX_SEED=0 && gradlew.bat run<ExperimentName>Graphic```
- If you're using PowerShell: ```$env:MAX_SEED = 0; .\gradlew.bat run<ExperimentName>Graphic``` \
  For the sake of simplicity, we will show Bash compatible commands below.
  Moreover, due to Alchemist's limitations, the graphical interface will not appear if run on a docker container.

The corresponding YAML simulation files to the experiments cited above are the following, with the default selection function set at `max`,
to change it to `min`, you can prepend the command with `FIND_MAX=false` as shown above:
- _gossip_: basic gossip experiment ```./gradlew runGossipGraphic```,
- _splitAndMergeNonStabGossip_: non-stabilizing min/max gossip in a split & merge topology ```./gradlew runSplitAndMergeNonStabGossipGraphic```,
- _splitAndMergeSelfStabGossip_: the proposed self-stabilizing min/max gossip in a split & merge topology ```./gradlew runSplitAndMergeSelfStabGossipGraphic```,
- _splitAndMergeTimeRepGossip_: non-stabilizing gossip wrapped by time replication for self-stabilization in a split & merge topology ```./gradlew runSplitAndMergeTimeRepGossipGraphic```.

**NOTE:**
The tasks above *in graphic mode* will run the experiments with the default parameters.

#### Changing experiment's parameters
To change the parameters of the experiments, you can modify the **YAML** files located in the `src/main/yaml` folder.
The parameters that can be changed are:
- `totalNodes`: the number of nodes in the simulation;
- `shouldFindMax`: the type of selector function used in the gossip protocol, `true` for `max`, `false` for `min`, and it is set to `true` by default.

Each change in the parameters will result in a different setup and execution of the experiment.
The parameters provided in the YAML files are the ones used for the evaluation and the ones evaluated as "optimal."

For further information about the YAML structure,
please refer to the [Alchemist documentation](https://alchemistsimulator.github.io/reference/yaml/index.html).

## Project Structure

```txt
experiments-coordination-self-stabilizing-gossip/
├── docker/                 # Dockerfiles to build containers
├── effects/                # Json specification for Alchemist's GUI visualization
├── gradle/                 # Gradle wrapper files
├─src/
│ └── main/
│     ├──kotlin/it/unibo/     # Kotlin source code for the experiments
│     │   ├── alchemist       # Alchemist's model and global reactions
│     │   │   ├── actions     # Alchemist actions for the experiments, such as the logic for node movement
│     │   │   ├── boundary/extractors   # Extractors for the experiments, such as the error extractor
│     │   │   └── collektive/device # Collektive device integration for Alchemist
│     └──collektive/
│        ├──alchemist/device/sensors/  # Sensors for the experiments, including random generator and time sensor
│        ├──experiments/      # Entrypoints for the experiments
│        ├──stdlib/           # Collektive's standard library for the experiments, including Gossip and Time Replication implementatio
│        └── utils            # Utility functions for the experiments
└── yaml/                     # YAML files for the experiments specification
```

## Simulation Entrypoints

The entrypoints for the experiments are located in the `src/main/kotlin/it/unibo/collektive/experiments` package.
Each experiment has its own entrypoint file, which is responsible for setting up the simulation environment and running the experiment,
namely `NonStabGossip.kt`, `SelfStabGossip.kt`, `TimeRepGossip.kt`.
Each file contains the function that is called by the Alchemist simulator, 
and that function will be executed each time stamp of the simulation.
Each experiment has some parameter, such as time sensor, that have to be properly configured in the YAML file of the experiment.
The time sensor is used to evaluate when is the right time to change the local value of the node.
The environment variable, instead, is responsible for obtaining data from the Alchemist simulator, 
such as the parameter responsible for the min/max evaluation strategy.

The entrypoints are quite straightforward, 
as they only call the corresponding method of the Collektive library, e.g. `gossipMin` and `timeReplicated`.

The logic of the experiments is implemented in the `src/main/kotlin/it/unibo/collektive/stdlib` package, 
where the `FindMaxOf.kt` and `TimeReplication.kt` files contain the implementation of the gossip protocol and the time replication strategy, respectively,
while the `nonStabilizingGossip` is taken from the latest published version of the Collektive library.

### Reproduce the experiment results

**WARNING**: re-running the whole experiment may take a very long time on a normal computer.

**Note** that the _gossip_ experiment is not included in the batch experiments, 
as it is a basic experiment used for demonstration purposes, 
and it is not used for data collection and analysis.

To collect the data for the analysis and the charts,
the experiments have to be run in "batch mode,"
which means that the experiments are run without the graphical interface,
and with different combinations of parameters.
Running the experiments in batch mode in a normal computer may take a very long time (e.g., days),
depending on the available hardware.
Moreover,
be sure that the YAML file of the experiment you want to run in batch mode
is properly configured with the desired parameters.

#### Reproduce the experiments with containers (recommended)

1. Install [Docker](https://www.docker.com/products/docker-desktop) and [docker-compose](https://docs.docker.com/compose/install/);
2. Run `docker-compose up` in the root folder of the repository:
   this will build the Docker images and run the containers needed to run the experiments.
3. From the `docker-compose.yml` file, you can see that three separate containers will be created, one for each experiment, and the data will be collected in the `data` folder.
   Note that the `volumes` field has to be updated to match your local environment.
You may need to adjust the `volumes` paths to match your machine.

#### Reproduce natively

1. Install a Gradle-compatible version of Java.
   Use the [Gradle/Java compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html)
   to learn which is the compatible version range.
   The Version of Gradle used in this experiment can be found in the `gradle-wrapper.properties` file
   located in the `gradle/wrapper` folder.
2. Install the version of Python indicated in `.python-version` (or use `pyenv`).
3. Launch either:
    - `./gradlew runAllBatch` on Linux, MacOS, or Windows if a bash-compatible shell is available;
    - `gradlew.bat runAllBatch` on Windows cmd or Powershell;
4. Once the experiment is finished, the results will be available in the `data` folder,
   **if data extraction is properly configured in the YAML files**.

#### Generate the charts

**WARNING**: depending on the amount of data collected, this process may take a long time.

1. Make sure you have Python 3.10 or higher installed.
2. The data folder structure should be the following:
    ```txt
    experiments-coordination-self-stabilizing-gossip/
    ├── data/
    │   ├── <experiment-name>/
    │   ├── <experiment-name2>/
    │   └── .../
    ```
3. Install the required Python packages by running:
    ```bash
    pip install --upgrade pip
    pip install -r requirements.txt
    ```
4. Run the script to process the data and generate the charts (this process may take some time):
    ```bash
    python plot.py
    ```
5. The charts will be generated in the `charts` folder.
6. If you want to regenerate the charts, you can run the script again.
