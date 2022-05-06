# Webis Query Interpretation

## Abstract 
Web search queries can be ambiguous: is "source of the nile'' meant to find information on the actual river or on a board game of that name? We tackle this problem by deriving entity-based query interpretations: given some query, the task is to derive all reasonable ways of linking suitable parts of the query to semantically compatible entities in a background knowledge base. Our suggested approach focuses on effectiveness but also on efficiency since web search response times should not exceed some hundreds of milliseconds. In our approach, we use query segmentation as a pre-processing step that finds promising segment-based "interpretation skeletons''. The individual segments from these skeletons are then linked to entities from a knowledge base and the reasonable combinations are ranked in a final step. An experimental comparison on a combined corpus of all existing query entity linking datasets shows our approach to have a better interpretation accuracy at a better run time than the previously most effective methods.

## Setup

### 1. Download data dumps

For entity linking tasks:
* Run `src/main/sh/install-el.sh` 

For query interpretation tasks:
* Run `src/main/sh/install-interpretation.sh`

### 2. Compile source code

To compile the source code you'll need to have Java 8 and Maven installed. 
Run the following commands for the compilation:

```
mvn clean compile 
mvn package
```

## Usage of the CLI

To run the CLI client execute:  
```
java -jar target/query-interpretation-1.0-SNAPSHOT-jar-with-dependencies.jar <command>
```

### Entity Linking

```
java -jar target/query-interpretation-1.0-SNAPSHOT-jar-with-dependencies.jar entitylink -q=<query>
```

### Interpretation

```
java -jar target/query-interpretation-1.0-SNAPSHOT-jar-with-dependencies.jar interpret -q=<query>
```

## Cite

If you want to use our tool please cite: 

```
@InProceedings{kasturia:2022,
  author =                {Vaibhav Kasturia and Marcel Gohsen and Matthias Hagen},
  booktitle =             {15th ACM International Conference on Web Search and Data Mining (WSDM 2022)},
  doi =                   {10.1145/3488560.3498532},
  month =                 feb,
  publisher =             {ACM},
  site =                  {Tempe, AZ, USA},
  title =                 {{Query Interpretations from Entity-Linked Segmentations}},
  url =                   {https://dl.acm.org/doi/10.1145/3488560.3498532},
  year =                  2022
}
```
