# Exercice commencé
Votre note est de **[(${{grade}})]**/[(${{maxGrade}})].  

## Détail[# th:each="part : ${gradeParts}"]
* [(${part.id})]: [(${{part.grade}})][# th:if="${part.maxGrade}"]/[(${{part.maxGrade}})][/][# th:each="comment : ${part.comments}"]
    * [(${comment})]
[/][/]


[# th:if="${deadline}"]Vous avez jusqu'à [(${deadline})] pour améliorer votre note.

[/]*Analyse effectuée à [(${now})].*
