import re

with open("app/src/main/java/com/aistudio/orbit/model/InvestigationStateMachine.kt", "r") as f:
    content = f.read()

def replacer(match):
    before = match.group(1)
    ev_count = match.group(2)
    conf_count = match.group(3)
    rec_action = match.group(4)
    after = match.group(5)

    return f'''{before}evidenceCount = {ev_count},
                confidence = {conf_count},
                recommendedNextAction = "{rec_action}",
                nextBestActionDetailed = NextBestAction(
                    action = "{rec_action}",
                    reason = "Derived from current state progression",
                    supportingEvidenceCount = {ev_count},
                    expectedInvestigativeValue = "High",
                    requiredInput = "Target Address",
                    targetStage = "Next available phase",
                    confidence = {conf_count},
                    costImplication = "Standard API limits"
                ){after}'''

# Match evidenceCount, confidence, recommendedNextAction
pattern = r'(missingInputs[^,]*,\s*)evidenceCount\s*=\s*(\d+|evidenceCount),\s*confidence\s*=\s*(\d+),\s*recommendedNextAction\s*=\s*"([^"]+)",\s*nextBestActionDetailed\s*=\s*NextBestAction\([^)]+\)(\s*\))'
content = re.sub(pattern, replacer, content)

with open("app/src/main/java/com/aistudio/orbit/model/InvestigationStateMachine.kt", "w") as f:
    f.write(content)
