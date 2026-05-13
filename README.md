# Projet_DBA
Projet de fin de session pour le cours de DBA

## Par Antoine Dextraze

## Justification
La base de données utilisée est Neo4J. J’ai utilisé celle-ci parce que je peux facilement effectuer mes recherches avec les relations, plus intuitif que de chercher dans des références de tableau. Il y a des relations de planète en planète ou de voisin qui sont nécessaires dans les recherches et Neo4J est parfait pour cela, pareil pour connaître le nombre de planètes par galaxie. Le concept de nœuds permet de réaliser plus facilement ces recherches qu’avec une base de données orientée documents. Par ailleurs dans l’une de mes requêtes, j’utilise shortestPath() qui est déjà intégré dans Neo4J et qui permet de chercher le chemin le plus court d’un nœud à un autre, donc le chemin le plus court d’une planète à une autre. Ce sont les relations entre les planètes qui sont le cœur de la logique à utiliser.

## Emplacement des index + type
Les index sont au début de mon fichier LogDAO dans le bloc d'initialisation statique( static ).
Se sont des index simples (Simple Property Index) non composite.

## IA
L'IA a été utilisée uniquement pour expliquer certaines erreurs d'inattention (ex. : asString au lieu de toString) et pour clarifier des concepts comme le fonctionnement d'OPTIONAL MATCH vs MATCH. Aucun code n'a été généré par l'IA — le code est entièrement écrit à la main.
