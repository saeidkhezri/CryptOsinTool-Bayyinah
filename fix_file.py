import sys

def main():
    with open("app/src/main/java/com/aistudio/orbit/ui/InvestigationViewModel.kt", "r") as f:
        lines = f.readlines()
        
    patch = []
    with open("patch_investigation.kt", "r") as f:
        patch = f.readlines()
        
    out = []
    i = 0
    while i < len(lines):
        line = lines[i]
        out.append(line)
        if "try {" in line:
            # check if the next lines match the patch
            match = True
            for j in range(len(patch)):
                if i + 1 + j >= len(lines):
                    match = False
                    break
                if lines[i + 1 + j].strip() != patch[j].strip():
                    match = False
                    break
            if match:
                # skip the patch
                i += len(patch)
        i += 1
        
    with open("app/src/main/java/com/aistudio/orbit/ui/InvestigationViewModel.kt", "w") as f:
        f.writelines(out)

if __name__ == "__main__":
    main()
