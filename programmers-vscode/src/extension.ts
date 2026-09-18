import * as vscode from 'vscode';
import * as path from 'path';
import * as fs from 'fs';
import { CliResult, buildCliWithGradle, findCli, runImport } from './cliRunner';

const PROBLEM_URL_PATTERN = /^https:\/\/school\.programmers\.co\.kr\/learn\/courses\/\d+\/lessons\/(\d+)(?:[/?#].*)?$/;

export function activate(context: vscode.ExtensionContext) {
    context.subscriptions.push(
        vscode.commands.registerCommand('programmers.importProblem', importProblemCommand),
        vscode.commands.registerCommand('programmers.importProblemFromClipboard', importFromClipboardCommand)
    );
}

export function deactivate() {
    // no-op
}

async function importProblemCommand(): Promise<void> {
    const url = await vscode.window.showInputBox({
        title: 'Enter Programmers Problem URL',
        placeHolder: 'https://school.programmers.co.kr/learn/courses/30/lessons/468381',
        ignoreFocusOut: true,
        validateInput: (value) => (PROBLEM_URL_PATTERN.test(value.trim()) ? undefined : '지원하는 Programmers 문제 URL 형식이 아닙니다.')
    });
    if (!url) {
        return;
    }
    await importProblem(url.trim());
}

async function importFromClipboardCommand(): Promise<void> {
    const clipboard = (await vscode.env.clipboard.readText()).trim();
    if (!PROBLEM_URL_PATTERN.test(clipboard)) {
        vscode.window.showErrorMessage('Clipboard does not contain a supported Programmers URL.');
        return;
    }
    await importProblem(clipboard);
}

async function importProblem(url: string, force = false): Promise<void> {
    const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
    if (!workspaceFolder) {
        vscode.window.showErrorMessage('Programmers Import를 사용하려면 워크스페이스 폴더를 열어야 합니다.');
        return;
    }
    const workspaceRoot = workspaceFolder.uri.fsPath;

    const cliPath = await resolveCliOrOfferBuild(workspaceRoot);
    if (!cliPath) {
        return;
    }

    const outputPath = resolveOutputPath(workspaceRoot);
    const problemId = extractProblemId(url);

    const result = await vscode.window.withProgress(
        {
            location: vscode.ProgressLocation.Notification,
            title: `Programmers 문제 ${problemId ?? ''} 불러오는 중...`
        },
        () => runImport(cliPath, url, outputPath, force)
    );

    if (result.code === 0) {
        await onImportSuccess(result, outputPath);
    } else {
        await onImportFailure(result, url, outputPath, problemId);
    }
}

async function resolveCliOrOfferBuild(workspaceRoot: string): Promise<string | undefined> {
    const found = findCli(workspaceRoot);
    if (found) {
        return found;
    }

    const gradlewName = process.platform === 'win32' ? 'gradlew.bat' : 'gradlew';
    const hasGradlew = fs.existsSync(path.join(workspaceRoot, gradlewName));
    const buildAction = 'Gradle로 빌드';
    const choice = await vscode.window.showErrorMessage(
        'programmers CLI를 찾을 수 없습니다. programmers-cli를 먼저 빌드해야 합니다.',
        ...(hasGradlew ? [buildAction] : [])
    );
    if (choice !== buildAction) {
        return undefined;
    }

    const built = await buildCliWithGradle(workspaceRoot);
    if (!built) {
        return undefined;
    }
    return findCli(workspaceRoot);
}

function resolveOutputPath(workspaceRoot: string): string {
    const configured = vscode.workspace.getConfiguration('programmers').get<string>('outputPath') ?? '';
    return configured ? path.join(workspaceRoot, configured) : workspaceRoot;
}

async function onImportSuccess(result: CliResult, outputPath: string): Promise<void> {
    const lines = result.stdout.split('\n').map((line) => line.trim()).filter(Boolean);
    vscode.window.showInformationMessage(lines.join(' / ') || 'Programmers problem imported.');

    const openAfter = vscode.workspace.getConfiguration('programmers').get<boolean>('openSolutionAfterImport');
    if (!openAfter) {
        return;
    }

    const solutionLine = lines.find((line) => line.endsWith('Solution.java'));
    if (!solutionLine) {
        return;
    }
    const relativeOrAbsolute = solutionLine.replace(/^✓\s*생성:\s*/, '').trim();
    const resolved = path.isAbsolute(relativeOrAbsolute) ? relativeOrAbsolute : path.join(outputPath, relativeOrAbsolute);
    await openFileIfExists(resolved);
}

async function onImportFailure(
    result: CliResult,
    url: string,
    outputPath: string,
    problemId: string | undefined
): Promise<void> {
    const errorLine = result.stderr.trim().split('\n').filter(Boolean).pop() ?? result.stderr.trim();
    const match = /^\[(\w+)]\s*(.*)$/.exec(errorLine);
    const code = match?.[1];
    const message = match?.[2] ?? errorLine;

    if (code === 'FILE_ALREADY_EXISTS') {
        const choice = await vscode.window.showWarningMessage(
            `Programmers problem ${problemId ?? ''} already exists.`,
            'Open', 'Overwrite', 'Cancel'
        );
        if (choice === 'Overwrite') {
            await importProblem(url, true);
        } else if (choice === 'Open' && problemId) {
            const solutionPath = path.join(
                outputPath, 'src', 'main', 'java', 'programmers', `p${problemId}`, 'Solution.java'
            );
            await openFileIfExists(solutionPath);
        }
        return;
    }

    vscode.window.showErrorMessage(
        `Failed to parse Programmers problem.\n${message || 'The page structure may have changed.'}`
    );
}

async function openFileIfExists(filePath: string): Promise<void> {
    if (!fs.existsSync(filePath)) {
        return;
    }
    const document = await vscode.workspace.openTextDocument(filePath);
    await vscode.window.showTextDocument(document);
}

function extractProblemId(url: string): string | undefined {
    return PROBLEM_URL_PATTERN.exec(url)?.[1];
}
